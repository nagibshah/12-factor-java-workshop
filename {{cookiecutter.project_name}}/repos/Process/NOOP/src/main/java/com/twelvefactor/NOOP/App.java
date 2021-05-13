package com.twelvefactor.NOOP;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<NumberPlateTrigger, Object> {
    private final S3Client s3Client;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        s3Client = DependencyFactory.s3Client();
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
    }

    @Override
    public NumberPlateTrigger handleRequest(final NumberPlateTrigger payload, final Context context) {
        // TODO: invoking the api call using s3Client.
        logger.info(String.format("Process NOOP: process has started. Request= %s",gson.toJson(payload)));
        String msg = "";
        if (payload.numberPlate.detected) {
            /////////////////////////////////////////////////////////////
            //
            // TODO: Read the credit value from the database and decrement it
            //
            /////////////////////////////////////////////////////////////
            msg = String.format("Number plate %s was charged $ %d.",payload.numberPlate.numberPlateString,payload.charge);
        } else {
            msg = String.format("Number plate %s was not found. This will require manual resolution.",payload.numberPlate.numberPlateString);
            /////////////////////////////////////////////////////////////
            //
            // TODO: Return 'errorUnknownNumberPlate' error
            //
            /////////////////////////////////////////////////////////////
        }
        logger.info(msg);
        return payload;
    }
}
