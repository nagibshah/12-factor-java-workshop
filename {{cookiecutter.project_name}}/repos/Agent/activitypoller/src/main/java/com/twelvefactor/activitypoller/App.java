package com.twelvefactor.activitypoller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.GetActivityTaskRequest;
import software.amazon.awssdk.services.sfn.model.GetActivityTaskResponse;
import software.amazon.awssdk.utils.StringUtils;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */

public class App implements RequestHandler<Object, Object> {
    private final S3Presigner s3Presigner;
    private final SesClient sesClient;
    private final SfnClient sfnClient;
    private final DynamoDbClient dynamoDbClient;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        s3Presigner = DependencyFactory.s3Presigner();
        sesClient = DependencyFactory.sesClient();
        sfnClient = DependencyFactory.sfnClient();
        dynamoDbClient = DependencyFactory.dynamoDbClient();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public Object handleRequest(final Object input, final Context context) {
        //ExecutorService taskExecutor = Executors.newFixedThreadPool(2);
        ExecutorService taskExecutor = Executors.newFixedThreadPool(2);
        List<Callable<String>> callableTasks = new ArrayList<>();
        callableTasks.add(new InsufficientCreditHandler(System.getenv("StepFunctionActivityInsufficientCredit"), context));
        callableTasks.add(new UnknownNumberPlateHandler(System.getenv("StepFunctionActivityManualPlateInspection"), context));
        List<Future<String>> results;

        try {
            logger.info("Starting tasks..");
            results = taskExecutor.invokeAll(callableTasks);
            logger.info(String.format("Job 1 status: %s and Job 2 status: %s",results.get(1).get(), results.get(2).get()));
            //logger.info("Tasks Completed successfully.")
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing tasks with message: " + e.getMessage());
        }
        taskExecutor.shutdown();
        return input;
    }

    public class InsufficientCreditHandler implements Callable<String> {
        private final String insufficientCreditActivityARN;
        private final Context context;
        private final S3Presigner s3Presigner;
        private final SesClient sesClient;
        private final SfnClient sfnClient;
        private final DynamoDbClient dynamoDbClient;

        InsufficientCreditHandler(String insufficientCreditActivityARN, final Context context) {
            this.insufficientCreditActivityARN = insufficientCreditActivityARN;
            this.context = context;
            s3Presigner = DependencyFactory.s3Presigner();
            sesClient = DependencyFactory.sesClient();
            sfnClient = DependencyFactory.sfnClient();
            dynamoDbClient = DependencyFactory.dynamoDbClient();
        }

        public String call() {
            String result = null;
            try {
                logger.info("Getting activity task " + this.insufficientCreditActivityARN);
                GetActivityTaskResponse response = sfnClient.getActivityTask(GetActivityTaskRequest.builder()
                        .activityArn(this.insufficientCreditActivityARN)
                        .workerName("insufficient-credit-worker")
                        .build());

                if (HttpStatusCode.OK == response.sdkHttpResponse().statusCode() && !StringUtils.isEmpty(response.taskToken())) {
                    // task is found
                    logger.info(String.format("InsufficientCreditHandler: Found a task. Input is: %s", response.input()));
                    Type type = new TypeToken<NumberPlateTrigger>() {
                    }.getType();
                    NumberPlateTrigger input = gson.fromJson(response.input(), type);

                    if (!input.numberPlate.detected) {
                        logger.info("TESTING::input.numberPlate.detected is false which means this must be a test");
                        logger.info("Forcing number plate to test value");
                        input.numberPlate.detected = true;
                        input.numberPlate.numberPlateString = "TESTPLATE";
                    }

                    // Sign Image URL - pre-signed url for access
                    logger.info("Generating pre-signed url for image");
                    String imageLink = GetPreSignedUrl(input);
                    //
                    // Query DynamoDB to get the owner email
                    //
                    Map<String, AttributeValue> document = GetRecord(input.numberPlate.numberPlateString);
                    // generate and send email
                    String mailTo = document.get("ownerEmail").s();
                    String subject = "[ACTION] - Your account credit is exhausted";
                    String firstname = document.get("ownerFirstName").s();
                    String lastname = document.get("ownerLastName").s();
                    String taskToken = URLEncoder.encode(response.taskToken(), StandardCharsets.UTF_8.toString());

                    Content textContent = Content.builder()
                            .charset("UTF-8")
                            .data(String.format("Hello %1$s %2$s, Your vehicle with number plate: %3$s was recently detected on a toll road, but your account has insufficient credit to pay the toll." +
                                            "Please update your account balance immediately to avoid a fine." +
                                            "Please access this link to top up: %4$stopup/%3$s?taskToken=%5$s" +
                                            ".. Thanks. Toll Road Administrator.",
                                    firstname,
                                    lastname,
                                    input.numberPlate.numberPlateString,
                                    System.getenv("APIGWEndpoint"),
                                    taskToken
                            ))
                            .build();
                    Content htmlContent = Content.builder()
                            .charset("UTF-8")
                            .data(String.format("Hello %1$s %2$s,<br/><br/>Your vehicle with number plate <b>%3$s</b> was recently detected on a toll road, but your account has insufficient credit to pay the toll.<br/><br/>" +
                                            "<img src='%4$s'/><br/><a href='%4$s'>Click here to see the original image</a><br/><br/>" +
                                            "Please update your account balance immediately to avoid a fine. <br/>" +
                                            "<a href='%5$stopup/%3$s?taskToken=%6$s'><b>Click this link to top up your account now.</b></a><br/>" +
                                            "<br/><br/> Thanks<br/><b>Toll Road Administrator.</b><br/><br/>",
                                    firstname,
                                    lastname,
                                    input.numberPlate.numberPlateString,
                                    imageLink,
                                    System.getenv("APIGWEndpoint"),
                                    taskToken
                            ))
                            .build();
                    result = sendMail(subject, mailTo, textContent, htmlContent);
                }
            } catch (Exception e) {
                logger.error(String.format("Failed to process the request with error: %s", e.getMessage()));
                System.exit(1);
            }
            return result;
        }
    }

    public class UnknownNumberPlateHandler implements Callable<String> {
        private final String unknownNumberActivityARN;
        private final Context context;
        private final S3Presigner s3Presigner;
        private final SesClient sesClient;
        private final SfnClient sfnClient;
        private final DynamoDbClient dynamoDbClient;

        UnknownNumberPlateHandler(String unknownNumberActivityARN, final Context context) {
            this.unknownNumberActivityARN = unknownNumberActivityARN;
            this.context = context;
            s3Presigner = DependencyFactory.s3Presigner();
            sesClient = DependencyFactory.sesClient();
            sfnClient = DependencyFactory.sfnClient();
            dynamoDbClient = DependencyFactory.dynamoDbClient();
        }

        public String call() {
            String result = "";
            try {
                logger.info("Getting activity task " + this.unknownNumberActivityARN);
                GetActivityTaskResponse response = sfnClient.getActivityTask(GetActivityTaskRequest.builder()
                        .activityArn(this.unknownNumberActivityARN)
                        .workerName("unknown-number-plate-worker")
                        .build());
                if (HttpStatusCode.OK == response.sdkHttpResponse().statusCode() && !StringUtils.isEmpty(response.taskToken())) {
                    logger.info(String.format("ManualAdminTaskHandler: Found a task. Input is: %s",response.input()));
                    Type type = new TypeToken<NumberPlateTrigger>(){}.getType();
                    NumberPlateTrigger input = gson.fromJson(response.input(),type);
                    // sign the image url
                    logger.info("Generating pre-signed url for image");
                    String imageLink = GetPreSignedUrl(input);
                    // generate and send email
                    String mailTo = System.getenv("TargetEmailAddress");
                    String subject = "[ACTION] - Manual Decision Required!";
                    String taskToken = URLEncoder.encode(response.taskToken(), StandardCharsets.UTF_8.toString());

                    Content textContent = Content.builder()
                            .charset("UTF-8")
                            .data(String.format("Hello %1$s, An image was captured at a toll booth, " +
                                            "but the Number Plate Processor could not be confident that it could determine the actual number plate on the vehicle. We need your help to take a look at the image," +
                                            "and make a determination." +
                                            "Please access this link to take a decision: %3$sparse/%4$s/%5$s/5?imageLink=%7$s&taskToken=%6$s" +
                                            " .. Thanks. Toll Road Administrator",
                                    mailTo,
                                    imageLink,
                                    System.getenv("APIGWEndpoint"),
                                    input.bucket,
                                    input.key,
                                    taskToken,
                                    URLEncoder.encode(imageLink,StandardCharsets.UTF_8.toString())
                            ))
                            .build();
                    Content htmlContent = Content.builder()
                            .charset("UTF-8")
                            .data(String.format("Hello %1$s,<br/><br/> An image was captured at a toll booth, " +
                                            "but the Number Plate Processor could not be confident that it could determine the actual number plate on the vehicle. We need your help to take a look at the image," +
                                            "and make a determination.<br/><br/>" +
                                            "<img src='%2$s'/><br/><a href='%2$s'>Click here to see the original image if it is not appearing in the email correctly.</a><br/><br/>" +
                                            "<a href='%3$sparse/%4$s/%5$s/5?imageLink=%7$s&taskToken=%6$s'><b>Click this link to help assess the image and provide the number plate.</b></a><br/>" +
                                            "<br/><br/>Thanks<br/><b>Toll Road Administrator.</b><br/><br/>",
                                    mailTo,
                                    imageLink,
                                    System.getenv("APIGWEndpoint"),
                                    input.bucket,
                                    input.key,
                                    taskToken,
                                    URLEncoder.encode(imageLink,StandardCharsets.UTF_8.toString())
                            ))
                            .build();
                    result = sendMail(subject,mailTo, textContent, htmlContent);
                }

            } catch (Exception e) {
                logger.error(String.format("Failed to process the request with error: %s", e.getMessage()));
                System.exit(1);
            }
            return result;
        }
    }

    public String sendMail(String subject, String emailTo, Content text, Content html) {
        String result;
        Message emailMsg = Message.builder()
                .subject(Content.builder().data(subject).build())
                .body(Body.builder()
                        .text(text)
                        .html(html)
                        .build())
                .build();
        SendEmailRequest sendRequest = SendEmailRequest.builder()
                .source(System.getenv("TargetEmailAddress"))
                .replyToAddresses(Collections.singletonList(System.getenv("TargetEmailAddress")))
                .destination(Destination.builder()
                        .toAddresses(Collections.singletonList(emailTo))
                        .build())
                .message(emailMsg)
                .build();

        logger.info(String.format("Sending email to %s", System.getenv("TargetEmailAddress")));
        SendEmailResponse sendEmailResponse = sesClient.sendEmail(sendRequest);
        if (sendEmailResponse.sdkHttpResponse().isSuccessful()) {
            logger.info("The email was successfully sent.");
            result = "success";
        } else {
            logger.error("Internal Error: The email could not be sent.");
            result = "error";
        }
        return result;
    }

    private String GetPreSignedUrl(NumberPlateTrigger input) {
        String imageLink = "";
        try {
            logger.info(String.format("Generating presigned url for bucket:%s object:%s", input.bucket, input.key));
            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder()
                            .bucket(input.bucket)
                            .key(input.key)
                            .build();
            GetObjectPresignRequest getObjectPresignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofDays(1)) // valid for 1 day
                            .getObjectRequest(getObjectRequest)
                            .build();
            // Generate the pre-signed request
            PresignedGetObjectRequest presignedGetObjectRequest =
                    s3Presigner.presignGetObject(getObjectPresignRequest);
            imageLink = presignedGetObjectRequest.url().toString();
            logger.info("s3 presigned url: " + presignedGetObjectRequest.url());
        } catch (S3Exception e) {
            logger.error(String.format("Error generating object url from s3 with message %s",e.getMessage()));
            System.exit(1);
        }
        return imageLink;
    }

    private Map<String, AttributeValue> GetRecord(String numberPlate) {
        Map<String,AttributeValue> returnedItem = new HashMap<>();
        String key = "numberPlate";
        HashMap<String, AttributeValue> keyToGet = new HashMap<>();
        keyToGet.put(key, AttributeValue.builder()
                .s(numberPlate).build());
        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(System.getenv("DDBTableName"))
                .build();
        logger.info(String.format("Querying ddb for plate: %s",numberPlate));
        try {
            returnedItem = dynamoDbClient.getItem(request).item();
            logger.info(String.format("ddb response: %s", returnedItem.toString()));
            if (returnedItem == null) {
                String msg = String.format("Number plate %s was not found. This will require manual resolution",numberPlate);
                logger.error(msg);
                throw new UnknownNumberPlateError(msg);
            }
        } catch (DynamoDbException | UnknownNumberPlateError e) {
            logger.info(String.format("Failed to query the dynamodb table with error: %s",e.getMessage()));
            System.exit(1);
        }

        return returnedItem;
    }

    public class UnknownNumberPlateError extends Exception {
        public UnknownNumberPlateError(String message) {
            super(message);
        }
    }

}
