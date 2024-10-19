package com.sitedictation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
class PerformanceService {

    private final DiffMatchPatchHelper diffMatchPatchHelper;
    private final SentenceRepository sentenceRepository;
    private final TransactionRepository transactionRepository;
//    private ConcurrentMap<PerfCacheKey, PerformanceData> cache;

    PerformanceService(DiffMatchPatchHelper diffMatchPatchHelper,
                       SentenceRepository sentenceRepository,
                       TransactionRepository transactionRepository) {
        this.diffMatchPatchHelper = diffMatchPatchHelper;
        this.sentenceRepository = sentenceRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostConstruct
    private void init() {
//        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
//        cacheBuilder.maximumSize(1000);
//        cacheBuilder.expireAfterWrite(1L, TimeUnit.HOURS);
//        cache = cacheBuilder.<PerfCacheKey, PerformanceData>build().asMap();
    }

    PerformanceData getCumulativePerformance(String userName, String language) {
        SortedMap<YearMonth, PerformanceData> performance = getPerformance(userName, language);
        return computeCumulativePerformanceData(performance.values(), null);
    }

    private PerformanceData computeCumulativePerformanceData(Collection<PerformanceData> performanceDatas, YearMonth to) {
        PerformanceData cumPerf = new PerformanceData(null);
        for (PerformanceData performanceData : performanceDatas) {
            if ((to != null) && performanceData.getYearMonth().isAfter(to)) {
                continue;
            }
            cumPerf.setSuccessesCount(cumPerf.getSuccessesCount() + performanceData.getSuccessesCount());
            cumPerf.setTransactionsCount(cumPerf.getTransactionsCount() + performanceData.getTransactionsCount());
            cumPerf.setCumLevDist(cumPerf.getCumLevDist() + performanceData.getCumLevDist());
            cumPerf.setNewSuccessesCount(cumPerf.getNewSuccessesCount() + performanceData.getNewSuccessesCount());
            cumPerf.setNewPositionsCount(cumPerf.getNewPositionsCount() + performanceData.getNewPositionsCount());
            cumPerf.setCumLevDistNew(cumPerf.getCumLevDistNew() + performanceData.getCumLevDistNew());
        }
        return cumPerf;
    }

    SortedMap<YearMonth, PerformanceData> getPerformance(String userName, String language) {
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndLanguage(userName, language);
        transactions.sort(Comparator.comparing(Transaction::getTimeMillis));
        SortedMap<YearMonth, PerformanceData> performanceDatasByYearMonth = new TreeMap<>();
        // TODO use countsForFileNames for conditional success rates per month, i.e. conversion from streak n to streak n+1
        Map<String, Integer> countsForFileNames = new HashMap<>();
        Map<String, Integer> streaksForFileNames = new HashMap<>();
        Set<String> transcripts = new HashSet<>();
        Set<Long> transactionTimeMillis = new HashSet<>();
        for (Transaction transaction : transactions) {
            if (!transactionTimeMillis.add(transaction.getTimeMillis())) {
                throw new RuntimeException("too many transactions at timeMillis " + transaction.getTimeMillis());
            }
            YearMonth yearMonth = transaction.getYearMonth();
            PerfCacheKey perfCacheKey = new PerfCacheKey(userName, language, yearMonth);
//            if (cache.containsKey(perfCacheKey)) {
//                performanceDatasByYearMonth.putIfAbsent(yearMonth, cache.get(perfCacheKey));
//                continue;
//            }
            PerformanceData performanceData = performanceDatasByYearMonth.computeIfAbsent(
                    yearMonth, k -> new PerformanceData(yearMonth));
            performanceData.incrementTransactionsCount();
            // without in-memory store, looking up Sentences one by one would be infeasible
            Sentence sentence = sentenceRepository.findByFileName(transaction.getFileName())
                    .orElseThrow(() -> new RuntimeException("no sentence for " + transaction.getFileName()));
            int levDist;
            if (transaction.getCorrect()) {
                performanceData.incrementSuccessesCount();
                levDist = 0;
            } else {
                levDist = diffMatchPatchHelper.computeLevenshteinDistance(transaction, sentence);
            }
            performanceData.incrementCumLevDist(levDist);
            Integer countForFileName = countsForFileNames.merge(transaction.getFileName(), 1, Integer::sum);
            if (countForFileName == 1) {
                performanceData.incrementNewPositionsCount();
                if (transaction.getCorrect()) {
                    performanceData.incrementNewSuccessesCount();
                }
                performanceData.incrementCumLevDistNew(levDist);
            } else if (countForFileName == 2) {
                if (transaction.getCorrect()) {
                    // TODO also measure second impression success
                }
            }
            if (transcripts.add(sentence.getTranscript())) {
                performanceData.incrementNewTranscriptsCount();
                if (transaction.getCorrect()) {
                    performanceData.incrementNewTranscriptsSuccessesCount();
                }
            }
            performanceData.addFileName(transaction.getFileName());
            if (transaction.getSessionId() != 0L) {
                performanceData.addSessionId(transaction.getSessionId());
            }
            updateStreak(transaction, streaksForFileNames, performanceData);
        }
        int cumTxns = 0;
        int cumPos = 0;
        for (Map.Entry<YearMonth, PerformanceData> entry : performanceDatasByYearMonth.entrySet()) {
            PerformanceData performanceData = entry.getValue();
            cumTxns += performanceData.getTransactionsCount();
            performanceData.setCumTransactionsCount(cumTxns);
            cumPos += performanceData.getNewPositionsCount();
            performanceData.setCumPositionsCount(cumPos);
        }
//        for (Map.Entry<YearMonth, PerformanceData> entry : performanceDatasByYearMonth.entrySet()) {
//            YearMonth yearMonth = entry.getKey();
//            if (!yearMonth.equals(YearMonth.now())) {
//                cache.putIfAbsent(new PerfCacheKey(userName, language, yearMonth), entry.getValue());
//            }
//        }
        if (cumTxns != transactions.size()) {
            throw new RuntimeException("Exception number of cumulative transactions for all performance periods");
        }
        return performanceDatasByYearMonth;
    }

    private static void updateStreak(Transaction transaction, Map<String, Integer> streaksForFileNames, PerformanceData performanceData) {
        Integer streak = streaksForFileNames.getOrDefault(transaction.getFileName(), 0);
        if (transaction.getCorrect()) {
            if (streak < 0) {
                streak = 0;
            }
            streak++;
        } else {
            if (streak > 0) {
                streak = 0;
            }
            streak--;
        }
        streaksForFileNames.put(transaction.getFileName(), streak);
        if (streak >= 4) {
            performanceData.incrementPlus4Count();
        }
        // successes -> days
        // 1 -> 1
        // 2 -> 10
        // 3 -> 30
        // 4 -> 180
    }

    @AllArgsConstructor
    @Getter
    private static class PerfCacheKey {
        private final String userName;
        private final String language;
        private final YearMonth yearMonth;
    }
}
