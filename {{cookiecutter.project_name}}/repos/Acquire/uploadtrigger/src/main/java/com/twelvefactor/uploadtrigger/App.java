package com.twelvefactor.uploadtrigger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Subsegment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;
import software.amazon.awssdk.utils.StringUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<S3EventNotification, String> {
    private final RekognitionClient rekognitionClient;
    private final AWSXRayRecorder xrayRecorder;
    private final SfnClient sfnClient;
    private final SecretsManagerClient secretsManagerClient;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private String regExNumberPlate;

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        rekognitionClient = DependencyFactory.rekognitionClient();
        xrayRecorder = DependencyFactory.awsxRayRecorder();
        sfnClient = DependencyFactory.sfnClient();
        secretsManagerClient = DependencyFactory.secretsManagerClient();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public String handleRequest(S3EventNotification event, Context ctx) {
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
            logger.info("regExNumberPlate is not yet populated. Calling getSecretFromSecretsManager()...");
            // get the plate from secrets manager
            String secretName = "/Staging/{{cookiecutter.project_name}}/Metadata";
            regExNumberPlate = this.getSecretFromSecretsManager(secretsManagerClient,secretName);
            logger.info("regExNumberPlate is " + regExNumberPlate);
        }

        // prepare data to be passed to the state machine
        // prepare data to be passed to the state machine
        NumberPlateTrigger result = new NumberPlateTrigger();
        result.setCharge(tollCharge);
        result.setBucket(srcBucket);
        result.setKey(srcKey);
        result.setContentType("");
        result.setContentLength(objectSize);
        NumberPlate numberPlate = new NumberPlate();
        numberPlate.setDetected(false);
        numberPlate.setNumberPlateString(regExNumberPlate);
        result.setNumberPlate(numberPlate);

        // distributed tracing segments and metadata
        Subsegment subsegment = xrayRecorder.beginSubsegment("TollGantry::Detect Number Plate in Captured Image");
        subsegment.putMetadata("bucket",event.getRecords().get(0).getS3().getBucket().getName());
        subsegment.putMetadata("key", event.getRecords().get(0).getS3().getObject().getKey());
        subsegment.putMetadata("regex",this.regExNumberPlate);

        // call rekognition to get the number plate
        List<TextDetection> textCollection = Collections.emptyList();
        try {
            S3Object s3Object = S3Object.builder()
                    .name(srcKey)
                    .bucket(srcBucket).build();
            Image plateImg = Image.builder().s3Object(s3Object).build();
            DetectTextRequest detectTextRequest = DetectTextRequest.builder()
                    .image(plateImg).build();
            logger.info("Calling Rekognition ...");
            DetectTextResponse response = rekognitionClient.detectText(detectTextRequest);
            textCollection = response.textDetections();
            logger.info(String.format("Response from Rekognition: %s",gson.toJson(response.textDetections())));
        } catch (RekognitionException e) {
            logger.error(String.format("Error invoking Rekognition with message: %s",e.getMessage()));
            System.exit(1);
        }

        // check if a valid number was detected...
        for (TextDetection textItem : textCollection) {
            if (!result.numberPlate.detected
                    && textItem.confidence() > Float.parseFloat(System.getenv("RekognitionTextMinConfidence"))
                    && textItem.type().name().equals("LINE")) {
                // Regex matches
                //List<String> allMatches = new ArrayList<String>();
                StringBuilder plateNumber = new StringBuilder();
                Matcher m = Pattern.compile(regExNumberPlate).matcher(textItem.detectedText());
                while (m.find()) {
                    plateNumber.append(m.group());
                    //allMatches.add(m.group());
                }
                if (!StringUtils.isEmpty(plateNumber.toString())) {
                    result.numberPlate.detected = true;
                    result.numberPlate.confidence = textItem.confidence();
                    result.numberPlate.numberPlateString = plateNumber.toString().replaceAll("\\s+",""); // remove whitespaces
                    logger.info(String.format("A valid plate number was detected %s", plateNumber));
                }
            }
        }

        xrayRecorder.endSubsegment();

        //
        // At this point, we either know it is a valid number plate
        // or it couldn't be determined with adequate confidence
        // so we need manual intervention
        //

        //
        // Kick off the step function
        //
        logger.info("Starting the state machine");
        // specify the name of the execution using a guid value
        String uuid = UUID.randomUUID().toString();
        ////////////////////////////////////////////////////////////
        //
        // TODO: add code to start the state machine
        //
        /////////////////////////////////////////////////////////////


        logger.info("Successfully processed s3 event.");
        return "Ok";
    }

    private String getSecretFromSecretsManager(SecretsManagerClient secretsClient, String secretName) {
        ////////////////////////////////////////////////////////////
        //
        // TODO: Call secrets manager to retrieve the plate number regex
        //
        /////////////////////////////////////////////////////////////
        return ".*";
    }

}
