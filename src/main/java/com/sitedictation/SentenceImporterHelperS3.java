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
import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("!local")
class SentenceImporterHelperS3 implements SentenceImporterHelper {

    private final AwsProperties awsProperties;
    private final S3Client s3Client;

    SentenceImporterHelperS3(AwsProperties awsProperties, S3Client s3Client) {
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
    }

    @Override
    public List<String> getCorpora() {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(awsProperties.getCorporaBucket())
                .build());
        return listObjectsResponse.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    @Override
    public InputStream getInputStream(String corpus) {
        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(awsProperties.getCorporaBucket())
                .key(corpus)
                .build());
        return objectAsBytes.asInputStream();
    }
}
