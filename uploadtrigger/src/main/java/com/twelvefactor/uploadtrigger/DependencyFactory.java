
package com.twelvefactor.uploadtrigger;

import com.amazonaws.xray.AWSXRayRecorder;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import com.amazonaws.xray.AWSXRay;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;


/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of S3Client
     */
    public static S3Client s3Client() {
        return S3Client.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.US_EAST_1)
                       .httpClientBuilder(UrlConnectionHttpClient.builder())
                       .build();
    }

    public static RekognitionClient rekognitionClient() {
        return RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public static SfnClient sfnClient() {
        return SfnClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public static SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public static AWSXRayRecorder awsxRayRecorder(){
        return AWSXRay.getGlobalRecorder();
    }
}
