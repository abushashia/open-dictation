package com.sitedictation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Component
@Profile("!local")
@Slf4j
class TransactionExporterHelperS3 implements TransactionExporterHelper {

    private final AwsProperties awsProperties;
    private final S3Client s3Client;

    TransactionExporterHelperS3(AwsProperties awsProperties, S3Client s3Client) {
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
    }

    @Override
    public void exportTransactions(String transactionsFileName, byte[] bytes) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(awsProperties.getTransactionsBucket())
                .key(transactionsFileName)
                .build();
        PutObjectResponse putObjectResponse = s3Client.putObject(
                objectRequest,
                RequestBody.fromBytes(bytes));
        if (!putObjectResponse.sdkHttpResponse().isSuccessful()) {
            log.warn("failed to put object to s3: {}", transactionsFileName);
        }
    }
}
