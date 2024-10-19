package com.sitedictation;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Component
@Profile("!local")
class AudioServiceHelperS3 implements AudioServiceHelper {

    private final CorpusMetadataHelper corpusMetadataHelper;
    private final S3Client s3Client;

    AudioServiceHelperS3(CorpusMetadataHelper corpusMetadataHelper, S3Client s3Client) {
        this.corpusMetadataHelper = corpusMetadataHelper;
        this.s3Client = s3Client;
    }

    @Override
    public byte[] getAudioPayload(String corpus, String fileName) {
        String bucket = corpusMetadataHelper.getBucketForCorpus(corpus);
        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build());
        return objectAsBytes.asByteArray();
    }
}
