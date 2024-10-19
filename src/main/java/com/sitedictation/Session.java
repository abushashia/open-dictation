package com.sitedictation;

import lombok.Data;

@Data
class Session {

    private Long sessionId;
    private String corpus;
    private int transactionsCount;
    private int successesCount;
    private int positionsCount;
    private int newPositionsCount;
    private int newSuccessesCount;
    // TODO consider percent right of total length of all transcripts, but you could have too much too
    private int newTranscriptsCount;
    private int newTranscriptSuccessesCount;
    private int totalLength;

    private int forceCorrectCount;
    private int reservationsCount;

    Session(Long sessionId) {
        this.sessionId = sessionId;
    }

    void incrementTransactionsCount() {
        transactionsCount++;
    }

    void incrementSuccessesCount() {
        successesCount++;
    }

    void incrementPositionsCount() {
        positionsCount++;
    }

    void incrementNewPositionsCount() {
        newPositionsCount++;
    }

    void incrementNewSuccessesCount() {
        newSuccessesCount++;
    }

    void incrementNewTranscriptsCount() {
        newTranscriptsCount++;
    }

    void incrementNewTranscriptSuccessesCount() {
        newTranscriptSuccessesCount++;
    }

    void incrementTotalLength(int increase) {
        totalLength += increase;
    }

    public double getSuccessRate() {
        return successesCount / (1.0 * transactionsCount);
    }

    public Double getSuccessRateOld() {
        int oldSuccesses = successesCount - newSuccessesCount;
        int oldTransactions = transactionsCount - newPositionsCount;
        return oldSuccesses / (1.0 * oldTransactions);
    }

    public Double getSuccessRateNew() {
        if (newPositionsCount == 0) {
            return null;
        }
        return newSuccessesCount / (1.0 * newPositionsCount);
    }

    public Double getSuccessRateNewTranscript() {
        if (newTranscriptsCount == 0) {
            return null;
        }
        return newTranscriptSuccessesCount / (1.0 * newTranscriptsCount);
    }

    public int getErrorsCount() {
        return transactionsCount - successesCount;
    }

    public double getAverageLength() {
        return totalLength / (transactionsCount * 1.0);
    }
}
