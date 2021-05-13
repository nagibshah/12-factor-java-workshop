package com.twelvefactor.NOOP;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Subsegment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import java.util.UUID;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<S3Event, String> {
    private final SfnClient sfnClient;
    private final AWSXRayRecorder xrayRecorder;
    private final SecretsManagerClient secretsManagerClient;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private String regExNumberPlate;

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        sfnClient = DependencyFactory.sfnClient();
        xrayRecorder = DependencyFactory.awsxRayRecorder();
        secretsManagerClient = DependencyFactory.secretsManagerClient();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public String handleRequest(S3Event event, Context ctx) {
        logger.info("EVENT Received: " + gson.toJson(event));
        String srcKey, srcBucket;
        Long objectSize;
        int tollCharge;

        // pull the relevant info out from the event
        try {
            S3EventNotification.S3EventNotificationRecord record=event.getRecords().get(0);
            srcKey = record.getS3().getObject().getUrlDecodedKey();
            srcBucket = record.getS3().getBucket().getName();
            objectSize = record.getS3().getObject().getSizeAsLong();
            tollCharge = Integer.parseInt(System.getenv("TollgateCharge"));

            logger.info(String.format("Bucket Name is: %s",record.getS3().getBucket().getName()));
            logger.info(String.format("File Path is %s",record.getS3().getObject().getKey()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (regExNumberPlate == null) {
            logger.info("regExNumberPlate is not yet populated. Calling getNumberPlateFromSecretsManager()...");
            // get the plate from secrets manager
            String secretName = "/Staging/{{cookiecutter.project_name}}/Metadata";
            regExNumberPlate = this.getSecretFromSecretsManager(secretsManagerClient,secretName);
            logger.info("regExNumberPlate is " + regExNumberPlate);
        }

        // prepare data to be passed to the state machine
        NumberPlateTrigger result = new NumberPlateTrigger(srcBucket, srcKey, "", objectSize, tollCharge);
        result.numberPlate = result.new NumberPlate(regExNumberPlate, false);

        // distributed tracing segments and metadata
        Subsegment subsegment = xrayRecorder.beginSubsegment("TollGantry::Detect Number Plate in Captured Image");
        subsegment.putMetadata("bucket",event.getRecords().get(0).getS3().getBucket().getName());
        subsegment.putMetadata("key", event.getRecords().get(0).getS3().getObject().getKey());
        subsegment.putMetadata("regex",this.regExNumberPlate);

        //
        // TODO: Call Rekognition to detect text in the captured image and verify if valid number plate
        //

        xrayRecorder.endSubsegment();

        //
        // Kick off the step function
        //
        logger.info("Starting the state machine");
        // specify the name of the execution using a guid value
        String uuid = UUID.randomUUID().toString();
        try {
            StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                    .input(gson.toJson(result))
                    .stateMachineArn(System.getenv("NumberPlateProcessStateMachine"))
                    .name(uuid)
                    .build();
            StartExecutionResponse executionResponse = sfnClient.startExecution(executionRequest);
            logger.info(String.format("State Machine started with execution arn: %s",executionResponse.executionArn()));
        } catch (SfnException e) {
            logger.error(String.format("Failed to trigger the step function workflow with error: %s", e.getMessage()));
            System.exit(1);
        }


        logger.info("Successfully processed s3 event.");
        return "Ok";
    }

    private String getSecretFromSecretsManager(SecretsManagerClient secretsClient, String secretName) {
        //TODO: Call secrets manager to retrieve the plate number regex
        return ".*";
    }

}
