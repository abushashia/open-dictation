package com.sitedictation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "common-voice")
@Data
@Slf4j
class CommonVoiceProperties {

    private String language;
    private String directory;
}
