package com.sitedictation;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.bp.Duration;

import java.io.IOException;

@ConditionalOnProperty(prefix = "dictation", value = "translation-enabled", havingValue = "true")
@Configuration
class GoogleConfiguration {

    @Bean
    GoogleCredentials googleCredentials() throws IOException {
        // TODO make this a file separate from the jar
        // TODO upload google-cloud-credentials.json to s3 to support aws deployment
        // requires GOOGLE_APPLICATION_CREDENTIALS
        return GoogleCredentials.getApplicationDefault();
    }

    @Bean
    Translate translate(GoogleCredentials googleCredentials) {
        TranslateOptions translateOptions = TranslateOptions.newBuilder()
                .setCredentials(googleCredentials)
                .setRetrySettings(RetrySettings.newBuilder()
                        .setMaxAttempts(1)
                        .setTotalTimeout(Duration.ofSeconds(2L))
                        .build())
                .build();
        return translateOptions.getService();
    }
}
