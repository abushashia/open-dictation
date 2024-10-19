package com.sitedictation;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
@Profile("!local")
class TransactionImporterHelperS3 implements TransactionImporterHelper {

    private final AwsProperties awsProperties;
    private final S3Client s3Client;

    TransactionImporterHelperS3(AwsProperties awsProperties,
                                S3Client s3Client) {
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
    }

    @Override
    public Collection<String> getFileNames() {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(awsProperties.getTransactionsBucket())
                .build());
        return listObjectsResponse.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    @Override
    public InputStream getInputStream(String fileName) {
        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(awsProperties.getTransactionsBucket())
                .key(fileName)
                .build());
        return objectAsBytes.asInputStream();
    }
}
