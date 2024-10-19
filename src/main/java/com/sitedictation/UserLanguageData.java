package com.sitedictation;

import lombok.Data;

@Data
class UserLanguageData {

    private String language;
    private String languageDisplayName;
    private long sentences;
    private int positions;
    private long r4r;
    private int transactions;
    private Integer sessions;
    private Double success;
    private Integer userAudioFiles;
}
