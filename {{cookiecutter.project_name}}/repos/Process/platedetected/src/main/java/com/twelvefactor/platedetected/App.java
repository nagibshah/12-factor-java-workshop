package com.twelvefactor.platedetected;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<NumberPlateTrigger, Object> {
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        s3Client = DependencyFactory.s3Client();
        dynamoDbClient = DependencyFactory.dynamoDbClient();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public NumberPlateTrigger handleRequest(final NumberPlateTrigger payload, final Context context) {
        // TODO: invoking the api call using s3Client.
        logger.info(String.format("Plate Detected event: %s", gson.toJson(payload)));
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis()/1000); // seconds since the unix epoch
        if (rand.nextDouble() <= Double.parseDouble(System.getenv("RandomProcessingErrorProbability"))) {
            String msg = "Congratulations! A random processing error has occurred!";
            logger.info(msg);
            try {
                throw new RandomProcessingError(msg);
            } catch (RandomProcessingError randomProcessingError) {
                randomProcessingError.printStackTrace();
            }
        }

        Float credit = getAvailableCredit(dynamoDbClient,
                System.getenv("DDBTableName"),
                "numberPlate",
                payload.numberPlate.numberPlateString);
        if (credit > payload.charge) {
            // charge the customer
            chargeCustomer(payload.numberPlate.numberPlateString, credit, payload.charge);
        } else {
            String msg = String.format("Driver for number plate %s has insufficient credit %.2f for a charge of %d",
                    payload.numberPlate.numberPlateString,
                    credit,payload.charge);
            logger.error(msg);
            try {
                throw new InsufficientCreditError(msg);
            } catch (InsufficientCreditError insufficientCreditError) {
                insufficientCreditError.printStackTrace();
            }
        }

        return payload;
    }


    public class RandomProcessingError extends Exception {
        public  RandomProcessingError(String message) {
            super(message);
        }
    }

    public class InsufficientCreditError extends Exception {
        public InsufficientCreditError(String message) {
            super(message);
        }
    }

    public class UnknownNumberPlateError extends Exception {
        public UnknownNumberPlateError(String message) {
            super(message);
        }
    }

    // get available credit from dynamodb
    public Float getAvailableCredit(DynamoDbClient ddb,String tableName, String key, String keyVal) {
        HashMap<String, AttributeValue> keyToGet = new HashMap<String,AttributeValue>();
        keyToGet.put(key, AttributeValue.builder()
                .s(keyVal).build());
        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(tableName)
                .build();
        Float credit = 0.0f;

        try {
            // get the available credit value
            Map<String,AttributeValue> returnedItem = ddb.getItem(request).item();

            if (returnedItem != null) {
                credit = Float.parseFloat(returnedItem.get("credit").toString());
                logger.info(String.format("Available credit for plate:%s is %.2f",keyVal,credit));
            }
            else {
                String msg = String.format("Number plate %s was not found. This will require manual resolution",keyVal);
                logger.error(msg);
                throw new UnknownNumberPlateError(msg);
            }
            return credit;

        } catch (DynamoDbException | UnknownNumberPlateError e) {
            logger.info(String.format("Failed to query the dynamodb table with error: %s",e.getMessage()));
            System.exit(1);
        }
        return credit;
    }

    // charge the account
    public void chargeCustomer(String numberPlateString, Float credit, int charge) {
        String key = "numberPlate";
        String name = "credit";
        HashMap<String,AttributeValue> itemKey = new HashMap<String,AttributeValue>();
        itemKey.put(key, AttributeValue.builder().s(numberPlateString).build());

        HashMap<String, AttributeValueUpdate> updatedValues =
                new HashMap<String,AttributeValueUpdate>();
        // build the update payload with changes
        Float chargedAmount = credit - charge;
        updatedValues.put(name, AttributeValueUpdate.builder()
                .value(AttributeValue.builder().n(chargedAmount.toString()).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(System.getenv("DDBTableName"))
                .key(itemKey)
                .attributeUpdates(updatedValues)
                .conditionExpression(String.format("credit >= %d",charge)) // set optional expression parameters
                .build();

        // update the record
        try {
            dynamoDbClient.updateItem(request);
        } catch (DynamoDbException e) {
            logger.info(String.format("Failed to update the %s number plate record with error: %s",numberPlateString,e.getMessage()));
            System.exit(1);
        }
    }
}
