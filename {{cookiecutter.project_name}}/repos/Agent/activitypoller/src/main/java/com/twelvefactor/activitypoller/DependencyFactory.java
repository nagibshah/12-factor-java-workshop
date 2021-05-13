
package com.twelvefactor.activitypoller;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {
    private static final String currentRegion = "{{cookiecutter.AWS_region}}"; //{{cookiecutter.AWS_region}}
    private DependencyFactory() {}

    /**
     * @return an instance of S3Client
     */
    public static S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(currentRegion))
                .build();
    }

    public  static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(currentRegion))
                .build();
    }
    public static SfnClient sfnClient() {
        return SfnClient.builder()
                .region(Region.of(currentRegion))
                .build();
    }
    public static SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(currentRegion))
                .build();
    }
}
