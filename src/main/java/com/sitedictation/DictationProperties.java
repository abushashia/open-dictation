package com.sitedictation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "dictation")
@Data
@Slf4j
public class DictationProperties {

    private boolean importSentencesSlice;
    private String importSentencesLocalDirectory;
    private String importTransactionsLocalDirectory;
    private String focusLanguage;
    private boolean userAudioEnabled;
    private String userAudioLocalDirectory;

    private boolean oauth2Enabled;
    private String adminUserName;

    private String prefix;
    private boolean plusOneOrLessOnly;
    private boolean repeatEnabled;
    private int fiatPercent;
    private boolean distributeFiatPercent;
    private Duration sessionDuration;
    private boolean translationEnabled;
    private boolean imageGenerationEnabled;
}
