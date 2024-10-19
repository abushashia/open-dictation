package com.sitedictation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("!local")
class AwsConfiguration {

    private final AwsProperties awsProperties;

    AwsConfiguration(AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
    }

    @Bean
    S3Client getS3Client() {
        StaticCredentialsProvider staticCredentialsProvider =
                StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        awsProperties.getAccessKeyId(),
                        awsProperties.getSecretAccessKey()));
        return S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(staticCredentialsProvider)
                .build();
    }
}
