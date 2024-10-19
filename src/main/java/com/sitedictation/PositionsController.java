package com.sitedictation;

import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.persistence.Transient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("positions")
class PositionsController {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DictationProperties dictationProperties;
    private final PositionService positionService;
    private final SentenceRepository sentenceRepository;
    private final TransactionRepository transactionRepository;

    PositionsController(ApplicationEventPublisher applicationEventPublisher,
                        DictationProperties dictationProperties,
                        PositionService positionService,
                        SentenceRepository sentenceRepository,
                        TransactionRepository transactionRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.dictationProperties = dictationProperties;
        this.positionService = positionService;
        this.sentenceRepository = sentenceRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    String getPositions(@RequestParam String language,
                        @RequestParam(required = false) String prefix,
                        @RequestAttribute String userName,
                        @RequestAttribute Long currentTimeMillis,
                        Model model) {
        model.addAttribute("language", language);

        CorpusDetail noCorpusCorpusDetail = new CorpusDetail();
        model.addAttribute("languageSummary", noCorpusCorpusDetail);
        long sentencesCount = sentenceRepository.countByLanguage(language);
        model.addAttribute("sentencesCount", sentencesCount);
        noCorpusCorpusDetail.setSentencesCount(sentencesCount);

        // load positions
        List<Position> positions = positionService.getPositions(userName, language);

        model.addAttribute("positionsCount", positions.size());
        noCorpusCorpusDetail.setPositionsCount(positions.size());
        model.addAttribute("percentComplete", positions.size() / (1.0 *  sentencesCount));

        long r4rCount = positions.stream()
                .filter(p -> p.isReadyForReview(currentTimeMillis))
                .filter(p -> prefix == null || p.getFileName().startsWith(prefix))
                .count();
        model.addAttribute("r4rCount", r4rCount);
        noCorpusCorpusDetail.setR4rCount(r4rCount);
        long plus4Count = positions.stream()
                .filter(p -> p.getStreak() >= 4)
                .filter(p -> prefix == null || p.getFileName().startsWith(prefix))
                .count();
        model.addAttribute("plus4Count", plus4Count);
        noCorpusCorpusDetail.setPlus4OrMoreCount(plus4Count);

        // compute unique transcripts known to user
        List<Sentence> sentences = sentenceRepository.findAllByLanguage(language);
        Map<String, Sentence> sentencesByFileName = new HashMap<>();
        for (Sentence sentence : sentences) {
            sentencesByFileName.put(sentence.getFileName(), sentence);
        }
        Set<String> uniqueTranscripts = new HashSet<>();
        for (Position position : positions) {
            Sentence sentence = sentencesByFileName.get(position.getFileName());
            uniqueTranscripts.add(sentence.getTranscript());
        }
        noCorpusCorpusDetail.setUniqueTranscriptsCount(uniqueTranscripts.size());

        List<String> distinctCorpora = sentenceRepository.findDistinctCorpora(language);
        if (distinctCorpora.size() > 1) {
            List<CorpusDetail> corpusDetails = new ArrayList<>();
            for (String corpus : distinctCorpora) {
                List<Position> corpusPositions = positions.stream()
                        .filter(p -> p.getCorpus().equals(corpus))
                        .collect(Collectors.toList());
                CorpusDetail corpusDetail = new CorpusDetail();
                corpusDetails.add(corpusDetail);
                corpusDetail.setCorpus(corpus);
                corpusDetail.setLanguage(language);
                corpusDetail.setSentencesCount(sentenceRepository.countByCorpus(corpus));
                corpusDetail.setPositionsCount(corpusPositions.size());
                corpusDetail.setR4rCount(corpusPositions.stream()
                        .filter(p -> p.isReadyForReview(currentTimeMillis))
                        .count());
                corpusDetail.setPlus4OrMoreCount(corpusPositions.stream()
                        .filter(p -> p.getStreak() >= 4)
                        .count());
            }
            model.addAttribute("corpusDetails", corpusDetails);
        }

        Map<String, Integer> signedStreakBuckets = getSignedStreakBuckets(positions, null);
        model.addAttribute("streakBuckets", signedStreakBuckets);

        Map<String, Integer> signedStreakBucketsR4r = getSignedStreakBuckets(positions, currentTimeMillis);
        model.addAttribute("streakBucketsR4r", signedStreakBucketsR4r);

        model.addAttribute("conditionalSuccessRateDatas", getCumulativeStreakData(positions).values());

        final String effectivePrefix = (prefix != null) ? prefix : dictationProperties.getPrefix();
        if (effectivePrefix != null) {
            model.addAttribute("prefix", effectivePrefix);
            model.addAttribute("prefixPositionsCount", positions.stream()
                    .filter(p -> p.getFileName().startsWith(effectivePrefix))
                    .count());
            model.addAttribute("prefixR4rCount", positions.stream()
                    .filter(p -> p.getFileName().startsWith(effectivePrefix))
                    .filter(p -> p.isReadyForReview(currentTimeMillis))
                    .count());
            model.addAttribute("prefixPlus4Count", positions.stream()
                    .filter(p -> p.getFileName().startsWith(effectivePrefix))
                    .filter(p -> p.getStreak() >= 4)
                    .count());
        }

        // compute histogram of number of impressions to positions
        // note, higher number of txns implies high error rate
        SortedMap<Integer, HistogramDetail> histogramDetailsByBucket = new TreeMap<>();
        for (Position position : positions) {
            int bucket = position.getTransactionsCount();
            HistogramDetail histogramDetail = histogramDetailsByBucket
                    .computeIfAbsent(bucket, HistogramDetail::new);
            histogramDetail.setNumPos(histogramDetail.getNumPos() + 1);
            histogramDetail.setPercent(histogramDetail.getNumPos() / (1.0 * positions.size()));
            histogramDetail.getFileNames().add(position.getFileName());
        }
        // now compute the percentiles
        int cumPos = 0;
        List<String> fileNamesOfWorstPositions = new ArrayList<>();
        for (HistogramDetail histogramDetail : histogramDetailsByBucket.values()) {
            cumPos += histogramDetail.getNumPos();
            histogramDetail.setPercentile(cumPos / (1.0 * positions.size()));
            if (histogramDetail.getPercentile() >= 0.9) {
                // worst positions are those that require the most transactions --> high error rate
                fileNamesOfWorstPositions.addAll(histogramDetail.getFileNames());
            }
        }
        model.addAttribute("numTxnsHistogramDetails", histogramDetailsByBucket.values());
        if (fileNamesOfWorstPositions.size() > 100) {
            fileNamesOfWorstPositions = fileNamesOfWorstPositions.subList(0, 100);
        }
        model.addAttribute("fileNamesOfWorstPositions", fileNamesOfWorstPositions);

        // TODO need count of single impression positions with negative streak
        // because need some metric to determine when to shift fiat factor back to 10%

        return "positions";
    }

    private static LinkedHashMap<String, Integer> getSignedStreakBuckets(List<Position> positions, Long currentTimeMillis) {
        SortedMap<Integer, Integer> streakBuckets = positions.stream()
                .filter(p -> (currentTimeMillis == null) || p.isReadyForReview(currentTimeMillis))
                .collect(Collectors.toMap(
                        Position::getStreak,
                        p -> 1,
                        Integer::sum,
                        TreeMap::new));
        // map that preserves order of insertion
        LinkedHashMap<String, Integer> signedStreakBuckets = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : streakBuckets.entrySet()) {
            signedStreakBuckets.put(entry.getKey() > 0 ? "+" + entry.getKey() : entry.getKey().toString(), entry.getValue());
        }
        return signedStreakBuckets;
    }

    private static SortedMap<Integer, ConditionalSuccessData> getCumulativeStreakData(List<Position> positions) {
        SortedMap<Integer, ConditionalSuccessData> cumStreakDataMap = new TreeMap<>(Comparator.reverseOrder());
        for (Position position : positions) {
            Map<Integer, ConditionalSuccessData> streakDataMap = position.getConditionalSuccessDataMap();
            for (Map.Entry<Integer, ConditionalSuccessData> entry : streakDataMap.entrySet()) {
                ConditionalSuccessData positionConditionalSuccessData = entry.getValue();
                ConditionalSuccessData cumConditionalSuccessData = cumStreakDataMap
                        .computeIfAbsent(entry.getKey(), ConditionalSuccessData::new);
                cumConditionalSuccessData.incrementTransactionsCount(positionConditionalSuccessData.getTransactionsCount());
                cumConditionalSuccessData.incrementSuccessesCount(positionConditionalSuccessData.getSuccessesCount());
            }
        }
        return cumStreakDataMap;
    }

    @Data
    private static class HistogramDetail implements Comparable<HistogramDetail> {
        private int numTxns;
        private int numPos;
        private double percent;
        private double percentile;

        @Transient
        private final List<String> fileNames = new ArrayList<>();

        HistogramDetail(int numTxns) {
            this.numTxns = numTxns;
        }

        @Override
        public int compareTo(HistogramDetail other) {
            return numTxns - other.numTxns;
        }
    }

    @GetMapping("words")
    String getWords(@RequestParam String language,
                    @RequestAttribute String userName,
                    Model model) {
        model.addAttribute("language", language);

        List<Position> positions = positionService.getPositions(userName, language);
        Map<String, Integer> wordCounts = new HashMap<>();
        Map<Integer, Integer> wordCountBuckets = new TreeMap<>();
        Map<Integer, Set<String>> countsToWordSets = new TreeMap<>();

        SortedMap<YearMonth, Integer> newWordsPerMonth = new TreeMap<>();

        for (Position position : positions) {
            // TODO map position opening transaction to month
            YearMonth openingYearMonth = position.getOpeningYearMonth();

            String fileName = position.getFileName();
            Sentence sentence = sentenceRepository.findByFileName(fileName)
                    .orElseThrow(() -> new RuntimeException("no sentence for " + fileName));
            String transcriptLC = sentence.getTranscript().toLowerCase();
            String transcriptWithoutPunctuation = transcriptLC.replaceAll("[^\\p{L} ]", "");
            String[] wordsLC = transcriptWithoutPunctuation.split("\\s+");
            for (String wordLC : wordsLC) {
                Integer oldCount = wordCounts.getOrDefault(wordLC, 0);
                if (oldCount == 0) {
                    newWordsPerMonth.merge(openingYearMonth, +1, Integer::sum);
                }

                Integer newCount = wordCounts.merge(wordLC, +1, Integer::sum);
                if (oldCount > 0) {
                    wordCountBuckets.merge(oldCount, -1, Integer::sum);
                    Set<String> oldCountWords = countsToWordSets.get(oldCount);
                    oldCountWords.remove(wordLC);
                    if (oldCountWords.isEmpty()) {
                        countsToWordSets.remove(oldCount);
                    }
                }
                wordCountBuckets.merge(newCount, +1, Integer::sum);
                Set<String> newCountWords = countsToWordSets.computeIfAbsent(newCount, k -> new HashSet<>());
                newCountWords.add(wordLC);
            }
        }
        wordCountBuckets.entrySet().removeIf(e -> e.getValue() <= 0);
        model.addAttribute("positionsCount", positions.size());
        model.addAttribute("wordsCount", wordCounts.size());
        model.addAttribute("wordCountBuckets", wordCountBuckets.entrySet());

        List<WordDataWitness> wordDatasWithWitnessWitnesses = new ArrayList<>();
        for (Map.Entry<Integer, Set<String>> entry : countsToWordSets.entrySet()) {
            Integer wordCount = entry.getKey();
            Set<String> wordSet = entry.getValue();
            int howManyWordOccurredWordCountTimes = wordSet.size();
            String witness = wordSet.iterator().next();
            wordDatasWithWitnessWitnesses.add(new WordDataWitness(witness, wordCount, howManyWordOccurredWordCountTimes));
        }
        model.addAttribute("wordDatasWithWitnesses", wordDatasWithWitnessWitnesses);

        SortedMap<YearMonth, WordDataMonthly> wordsDataPerMonthDesc = new TreeMap<>(Comparator.reverseOrder());
        int cumWords = 0;
        for (Map.Entry<YearMonth, Integer> entry : newWordsPerMonth.entrySet()) {
            Integer newWords = entry.getValue();
            cumWords += newWords;
            WordDataMonthly wordDataMonthly = new WordDataMonthly(entry.getKey(), newWords, cumWords);
            wordsDataPerMonthDesc.put(entry.getKey(), wordDataMonthly);
        }
        model.addAttribute("wordsDataPerMonthDesc", wordsDataPerMonthDesc.values());

        return "words";
    }

    @GetMapping("reservations")
    String getReservedPositions(@RequestParam String language, @RequestAttribute String userName, Model model) {
        List<Position> positions = positionService.getPositions(userName, language);
        List<String> reservedPositionFileNames = positions.stream()
                .filter(Position::isReserved)
                .sorted(Comparator.comparing(Position::getReservedTimeMillis).reversed())
                .map(Position::getFileName)
                .toList();
        model.addAttribute("fileNames", reservedPositionFileNames);
        model.addAttribute("language", language);
        return "reservations";
    }

    @PostMapping("release")
    String release(@RequestParam String language,
                   @RequestParam String fileName,
                   @RequestAttribute String userName,
                   RedirectAttributes redirectAttributes) {
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndLanguageAndFileName(userName, language, fileName);
        SortedSet<Transaction> sortedByTimeMillisDesc = new TreeSet<>(Comparator.comparing(Transaction::getTimeMillis).reversed());
        sortedByTimeMillisDesc.addAll(transactions);
        Transaction newest = sortedByTimeMillisDesc.first();
        if ((newest.getReserved() == null) || !newest.getReserved()) {
            throw new RuntimeException("unexpected transaction to be released");
        }
        newest.setReserved(false);
        saveTransactionEtc(newest);
        redirectAttributes.addAttribute("language", language);
        return "redirect:/positions/reservations";
    }

    // dd85408fba4e6a4d4b861567ce561aa717da93ff77ab19c3babb22c9105bbc9d629a0bc381a24a1096d44672b1b4a47ffc267bd5f24cac383e526e5cf866345a.mp3
    // needs reserved, add API
    @GetMapping("position")
    String getPosition(@RequestParam String language,
                       @RequestParam(required = false) String fileName,
                       @RequestAttribute String userName,
                       @RequestAttribute Long currentTimeMillis,
                       Model model) {
        if (fileName != null) {
            Position position = positionService.getPosition(userName, language, fileName);
            if (position != null) {
                model.addAttribute("corpus", position.getCorpus());
                model.addAttribute("reserved", position.isReserved());
                model.addAttribute("txnsCount", position.getTransactionsCount());
                model.addAttribute("txns", position.isReadyForReview(currentTimeMillis));
            }
        }
        model.addAttribute("language", language);
        model.addAttribute("fileName", fileName);
        return "position";
    }

    @PostMapping("reserve")
    String reserve(@RequestParam String language,
                   @RequestParam String fileName,
                   @RequestAttribute String userName,
                   RedirectAttributes redirectAttributes) {
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndLanguageAndFileName(userName, language, fileName);
        SortedSet<Transaction> sortedByTimeMillisDesc = new TreeSet<>(Comparator.comparing(Transaction::getTimeMillis).reversed());
        sortedByTimeMillisDesc.addAll(transactions);
        Transaction newest = sortedByTimeMillisDesc.first();
        newest.setReserved(true);
        saveTransactionEtc(newest);
        redirectAttributes.addAttribute("language", language);
        redirectAttributes.addAttribute("fileName", fileName);
        return "redirect:/positions/position";
    }

    private void saveTransactionEtc(Transaction transaction) {
        transactionRepository.save(transaction);
        applicationEventPublisher.publishEvent(new TransactionSavedEvent(transaction, this));
    }
}
