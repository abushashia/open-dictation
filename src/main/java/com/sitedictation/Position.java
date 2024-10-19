package com.sitedictation;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

class Position {

    private final SortedSet<Transaction> sortedByTimeMillisAsc;
    private final SortedSet<Transaction> sortedByTimeMillisDesc;
    private Integer streak;
    private Long r4rTimeMillis;

    Position(List<Transaction> transactions) {
        SortedSet<Transaction> modifiableSortedSetAsc = new TreeSet<>(Comparator.comparing(Transaction::getTimeMillis));
        modifiableSortedSetAsc.addAll(transactions);
        if (modifiableSortedSetAsc.size() != transactions.size()) {
            throw new IllegalArgumentException("unexpected duplicate transactions for " + transactions.get(0).getFileName());
        }
        sortedByTimeMillisAsc = Collections.unmodifiableSortedSet(modifiableSortedSetAsc);
        SortedSet<Transaction> modifiableSortedSetDesc = new TreeSet<>(Comparator.comparing(Transaction::getTimeMillis).reversed());
        modifiableSortedSetDesc.addAll(transactions);
        if (modifiableSortedSetDesc.size() != transactions.size()) {
            throw new IllegalArgumentException("unexpected duplicate transactions for " + transactions.get(0).getFileName());
        }
        sortedByTimeMillisDesc = Collections.unmodifiableSortedSet(modifiableSortedSetDesc);
        r4rTimeMillis = getReadyForReviewTimeMillis();
    }

    String getLanguage() {
        return sortedByTimeMillisDesc.first().getLanguage();
    }

    String getFileName() {
        return sortedByTimeMillisDesc.first().getFileName();
    }

    String getCorpus() {
        return sortedByTimeMillisDesc.first().getCorpus();
    }

    /**
     * How many consecutive successes or failures?
     */
    int getStreak() {
        if (streak != null) {
            return streak;
        }
        Transaction first = sortedByTimeMillisDesc.first();
        Boolean flag = first.getCorrect();
        int streak = 0;
        for (Transaction transaction : sortedByTimeMillisDesc) {
            if (transaction.getCorrect().equals(flag)) {
                streak++;
            } else {
                break;
            }
        }
        if (!flag) {
            streak = -streak;
        }
        this.streak = streak;
        return streak;
    }

    Map<Integer, ConditionalSuccessData> getConditionalSuccessDataMap() {
        int streakBeforeTxn = 0;
        Map<Integer, ConditionalSuccessData> conditionalSuccessDataMap = new HashMap<>();
        for (Transaction transaction : sortedByTimeMillisAsc) {
            ConditionalSuccessData conditionalSuccessData = conditionalSuccessDataMap.
                    computeIfAbsent(streakBeforeTxn, ConditionalSuccessData::new);
            conditionalSuccessData.incrementTransactionsCount();
            if (transaction.getCorrect()) {
                conditionalSuccessData.incrementSuccessesCount();
            }
            // update streak for next iteration
            if (streakBeforeTxn == 0) {
                streakBeforeTxn = transaction.getCorrect() ? +1 : -1;
            } else if (streakBeforeTxn > 0) {
                if (transaction.getCorrect()) {
                    streakBeforeTxn++;
                } else {
                    streakBeforeTxn = -1;
                }
            } else {
                if (transaction.getCorrect()) {
                    streakBeforeTxn = 1;
                } else {
                    streakBeforeTxn--;
                }
            }
        }
        return conditionalSuccessDataMap;
    }

    /**
     * When will the position be ready for review?
     */
    Long getReadyForReviewTimeMillis() {
        if (r4rTimeMillis != null) {
            return r4rTimeMillis;
        }
        Transaction newest = sortedByTimeMillisDesc.first();
        int streak = getStreak();
        Duration timeToAdd;
        if (streak <= 0) {
            // consider 2 minutes, if 10 minute session
            timeToAdd = Duration.ofMinutes(5);
        } else if (streak == 1) {
            timeToAdd = Duration.ofDays(1);
        } else if (streak == 2) {
            timeToAdd = Duration.ofDays(10);
        } else if (streak == 3) {
            timeToAdd = Duration.ofDays(30);
        } else {
            timeToAdd = Duration.ofDays(180);
        }
        r4rTimeMillis = Instant.ofEpochMilli(newest.getTimeMillis())
                .plus(timeToAdd)
                .toEpochMilli();
        if (Objects.equals(true, newest.getReserved())) {
            r4rTimeMillis = Instant.now().plus(999, ChronoUnit.DAYS).toEpochMilli();
        }
        return r4rTimeMillis;
    }

    boolean isReadyForReview(long currentTimeMillis) {
        return getReadyForReviewTimeMillis() <= currentTimeMillis;
    }

    LocalDateTime getReadyForReviewLocalDateTime() {
        Instant instant = Instant.ofEpochMilli(getReadyForReviewTimeMillis());
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    int getTransactionsCount() {
        return sortedByTimeMillisDesc.size();
    }

    Set<Long> getSessionIds() {
        return sortedByTimeMillisDesc.stream()
                .map(Transaction::getSessionId)
                .collect(Collectors.toSet());
    }

    long getLastImpressionTimeMillis() {
        return sortedByTimeMillisDesc.first().getTimeMillis();
    }

    YearMonth getOpeningYearMonth() {
        return sortedByTimeMillisAsc.first().getYearMonth();
    }

    boolean isReserved() {
        Transaction newest = sortedByTimeMillisDesc.first();
        return (newest.getReserved() != null) && newest.getReserved();
    }

    Long getReservedTimeMillis() {
        if (!isReserved()) {
            return null;
        }
        Transaction newest = sortedByTimeMillisDesc.first();
        return newest.getTimeMillis();
    }
}
