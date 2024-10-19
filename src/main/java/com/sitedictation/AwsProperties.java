package com.sitedictation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws")
@Data
class AwsProperties {

    private String region;
    private String accessKeyId;
    private String secretAccessKey;

    private String corporaBucket;
    private String transactionsBucket;
}
