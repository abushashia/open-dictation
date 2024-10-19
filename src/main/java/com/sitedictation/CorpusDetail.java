package com.sitedictation;

import lombok.Data;

@Data
class CorpusDetail {

    private String corpus;
    private String language;
    private long sentencesCount;
    private int positionsCount;
    private int uniqueTranscriptsCount;
    private long r4rCount;
    private long plus4OrMoreCount;

    public long getUnknownCount() {
        return sentencesCount - positionsCount;
    }

    public double getPercentComplete() {
        return positionsCount / (sentencesCount * 1.0);
    }

    public double getPercentPlus4OrMore() {
        return plus4OrMoreCount / (sentencesCount * 1.0);
    }
}
