package com.sitedictation;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Data
class PerformanceData {

    private YearMonth yearMonth;
    private int successesCount;
    private int transactionsCount;
    private int cumTransactionsCount;
    private int cumLevDist;
    private int newSuccessesCount;
    private int newPositionsCount;
    private int newTranscriptsCount;
    private int newTranscriptsSuccessesCount;
    private int plus4Count;

    private int cumLevDistNew;

    private final Set<String> fileNames = new HashSet<>();
    private int cumPositionsCount;
    private final SortedSet<Long> sessionIds = new TreeSet<>();

    PerformanceData(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    void incrementSuccessesCount() {
        successesCount++;
    }

    void incrementTransactionsCount() {
        transactionsCount++;
    }

    void incrementCumLevDist(int increment) {
        cumLevDist += increment;
    }

    void incrementNewSuccessesCount() {
        newSuccessesCount++;
    }

    void incrementNewPositionsCount() {
        newPositionsCount++;
    }

    void incrementNewTranscriptsCount() {
        newTranscriptsCount++;
    }

    void incrementNewTranscriptsSuccessesCount() {
        newTranscriptsSuccessesCount++;
    }

    void incrementCumLevDistNew(int increment) {
        cumLevDistNew += increment;
    }

    void incrementPlus4Count() {
        plus4Count++;
    }

    public Double getSuccessRate() {
        if (transactionsCount == 0) {
            return null;
        }
        return successesCount / (1.0 * transactionsCount);
    }

    public Double getAvgLevDist() {
        if (transactionsCount == 0) {
            return null;
        }
        return cumLevDist / (1.0 * transactionsCount);
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
        return newTranscriptsSuccessesCount / (1.0 * newTranscriptsCount);
    }

    public Double getAvgLevDistNew() {
        if (newPositionsCount == 0) {
            return null;
        }
        return cumLevDistNew / (1.0 * newPositionsCount);
    }

    public int getPositionsCount() {
        return fileNames.size();
    }

    public int getCumPositionsCount() {
        return cumPositionsCount;
    }

    public void setCumPositionsCount(int cumPositionsCount) {
        this.cumPositionsCount = cumPositionsCount;
    }

    void addFileName(String fileName) {
        fileNames.add(fileName);
    }

    public Integer getSessionsCount() {
        if (sessionIds.isEmpty()) {
            return null;
        }
        return sessionIds.size();
    }

    void addSessionId(Long sessionId) {
        if ((sessionId == null) || (sessionId == 0L)) {
            return;
        }
        sessionIds.add(sessionId);
    }

    public Double getSessionsPerDay() {
        if (sessionIds.isEmpty()) {
            return null;
        }
        int divisor;
        if (yearMonth.equals(YearMonth.now())) {
            LocalDate dayOfMonth = yearMonth.atDay(1);
            // if user started playing mid-month, then use date of very first session, to compute days available
            LocalDate localDateOfFirstSession = getLocalDateOfFirstSession();
            if (localDateOfFirstSession.isAfter(dayOfMonth)) {
                dayOfMonth = localDateOfFirstSession;
            }
            LocalDate today = LocalDate.now();
            int days = 0;
            while (!dayOfMonth.isAfter(today)) {
                days++;
                dayOfMonth = dayOfMonth.plusDays(1);
            }
            divisor = days;
        } else {
            divisor = yearMonth.lengthOfMonth();
        }
        return sessionIds.size() / (1.0 * divisor);
    }

    private LocalDate getLocalDateOfFirstSession() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(sessionIds.first()), ZoneId.systemDefault()).toLocalDate();
    }

    public Double getTransactionsPerSession() {
        if (sessionIds.isEmpty()) {
            return null;
        }
        return transactionsCount / (1.0 * sessionIds.size());
    }
}
