package com.sitedictation;

import lombok.Data;

@Data
class ConditionalSuccessData {

    private int streak;
    private int transactionsCount;
    private int successesCount;

    ConditionalSuccessData(int streak) {
        this.streak = streak;
    }

    void incrementTransactionsCount() {
        transactionsCount++;
    }

    void incrementTransactionsCount(int increment) {
        transactionsCount += increment;
    }

    void incrementSuccessesCount() {
        successesCount++;
    }

    void incrementSuccessesCount(int increment) {
        successesCount += increment;
    }

    public double getSuccessRate() {
        return successesCount / (1.0 * transactionsCount);
    }
}
