package com.sitedictation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("dictation")
@Slf4j
class DictationController {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AudioService audioService;
    private final CorpusMetadataHelper corpusMetadataHelper;
    private final DictationProperties dictationProperties;
    private final DiffMatchPatchHelper diffMatchPatchHelper;
    private final ImageService imageService;
    private final PositionService positionService;
    private final SentenceRepository sentenceRepository;
    private final TransactionRepository transactionRepository;
    private final TranslationService translationService;

    private final Random random = new Random();

    DictationController(ApplicationEventPublisher applicationEventPublisher,
                        AudioService audioService,
                        CorpusMetadataHelper corpusMetadataHelper,
                        DictationProperties dictationProperties,
                        DiffMatchPatchHelper diffMatchPatchHelper,
                        ImageService imageService,
                        PositionService positionService,
                        SentenceRepository sentenceRepository,
                        TransactionRepository transactionRepository,
                        TranslationService translationService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.audioService = audioService;
        this.corpusMetadataHelper = corpusMetadataHelper;
        this.dictationProperties = dictationProperties;
        this.diffMatchPatchHelper = diffMatchPatchHelper;
        this.imageService = imageService;
        this.positionService = positionService;
        this.sentenceRepository = sentenceRepository;
        this.transactionRepository = transactionRepository;
        this.translationService = translationService;
    }

    @GetMapping
    String prepareTranscript(@RequestParam String language,
                             @RequestParam(required = false) String corpus,
                             @RequestParam(required = false) Long sessionId,
                             @RequestParam(required = false) Integer sessionFiatPercent,
                             @RequestAttribute Long currentTimeMillis,
                             @RequestAttribute String userName,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        validateLanguageAndCorpus(language, corpus);
        validateSessionFiatPercent(sessionFiatPercent);
        if (sessionId == null) {
            // TODO check for active session - a session for same language and userName not older than 25 mins - last txn...
            sessionId = currentTimeMillis;
            sessionFiatPercent = getSessionFiatPercent();
            log.info("Starting session {} with session fiat percent {}", sessionId, sessionFiatPercent);
        } else {
            Duration duration = Duration.ofMillis(currentTimeMillis - sessionId);
            if (duration.compareTo(dictationProperties.getSessionDuration()) >= 0) {
                redirectAttributes.addAttribute("language", language);
                return "redirect:/sessions";
            }
        }
        List<Position> positions = positionService.getPositions(userName, language, corpus);
        boolean hasUnknownSentences;
        if (StringUtils.isNotBlank(corpus)) {
            hasUnknownSentences = sentenceRepository.countByLanguageAndCorpus(language, corpus) > positions.size();
        } else {
            hasUnknownSentences = sentenceRepository.countByLanguage(language) > positions.size();
        }
        // TODO fast track is probably no review (fiat 100%) until 10K positions, then intense review with low fiat factor
        // this incorporates Raluca's suggestion of no grading, really, no repetition of what you got wrong 5 mins ago (coercive learning)
        Position r4rPosition = getPositionToReview(positions, currentTimeMillis, hasUnknownSentences, sessionFiatPercent);
        Sentence sentence;
        if (r4rPosition != null) {
            String fileName = r4rPosition.getFileName();
            sentence = sentenceRepository.findByFileName(fileName)
                    .orElseThrow(() -> new RuntimeException("no sentence for " + fileName));
        } else if (hasUnknownSentences) {
            Set<String> knownFileNames = positions.stream()
                    .map(Position::getFileName)
                    .collect(Collectors.toSet());
            List<Sentence> sentences;
            if (StringUtils.isNotBlank(corpus)) {
                sentences = sentenceRepository.findAllByLanguageAndCorpus(language, corpus);
            } else {
                sentences = sentenceRepository.findAllByLanguage(language);
            }
            sentences.removeIf(s -> knownFileNames.contains(s.getFileName()));
            sentence = sentences.get(random.nextInt(sentences.size()));
        } else {
            redirectAttributes.addAttribute("language", language);
            return "redirect:/sessions";
        }
        Model primaryAttributes = getPrimaryAttributes(language, corpus, sessionId, sessionFiatPercent);
        primaryAttributes.addAttribute("fileName", sentence.getFileName());
        model.addAttribute("primaryAttributes", primaryAttributes.asMap().entrySet());
        model.addAttribute("audioHeaderAndPayload",
                audioService.getAudioHeaderAndPayload(sentence.getCorpus(), sentence.getFileName()));
        model.addAttribute("rtl", corpusMetadataHelper.isRightToLeft(language));
        model.addAttribute("userAudioEnabled", dictationProperties.isUserAudioEnabled());
        model.addAttribute("imageGenerationEnabled", dictationProperties.isImageGenerationEnabled());
        // TODO add attribute governing autoplay, nullable
        return "prepare";
    }

    /**
     * Based on conversation with ChatGPT, try to simulate normal distribution,
     * but with higher probability of a "high number"
     */
    private int getSessionFiatPercent() {
        int mainValue;
        double probabilityOfHighNumber = 2 / 7.0; // Adjust as needed, 2 out of 7 represents twice a week
        double randomValue = random.nextDouble();
        int baseFiatPercent = dictationProperties.getFiatPercent();
        if (!dictationProperties.isDistributeFiatPercent()) {
            return baseFiatPercent;
        }
        int highFiatPercent = Math.max(baseFiatPercent, 80);

        if (randomValue < probabilityOfHighNumber) {
            // Generate a "very high" number (e.g., around 80)
            mainValue = (int) (random.nextGaussian() * 10 + highFiatPercent); // Mean: 80, Standard deviation: 10
        } else {
            // Generate a regular number (e.g., around 25)
            mainValue = (int) (random.nextGaussian() * 10 + baseFiatPercent); // Mean: 25, Standard deviation: 10
        }

        // Ensure the value is within the desired range
        if (mainValue < 15) {
            mainValue = 15;
        } else if (mainValue > 100) {
            mainValue = 100;
        }
        return mainValue;
    }

    private void validateSessionFiatPercent(Integer sessionFiatPercent) {
        if (sessionFiatPercent != null) {
            if (sessionFiatPercent < 0 || sessionFiatPercent > 100) {
                throw new RuntimeException("unexpected sessionFiatPercent: " + sessionFiatPercent);
            }
        }
    }

    private void validateLanguageAndCorpus(String language, String corpus) {
        if (corpus != null) {
            long count = sentenceRepository.countByLanguageAndCorpus(language, corpus);
            if (count == 0) {
                throw new RuntimeException(String.format("unexpected corpus, %s, for language, %s", corpus, language));
            }
        }
    }

    private Position getPositionToReview(List<Position> positions,
                                         long currentTimeMillis,
                                         boolean hasUnknownSentences,
                                         Integer sessionFiatPercent) {
        if (isFiat(hasUnknownSentences, sessionFiatPercent)) {
            return null;
        }
        String prefix = dictationProperties.getPrefix();
        List<Position> r4rPositions = positions.stream()
                .filter(p -> p.isReadyForReview(currentTimeMillis))
                .filter(p -> (prefix == null) || p.getFileName().startsWith(prefix))
                .collect(Collectors.toList());
        if (dictationProperties.isPlusOneOrLessOnly()) {
            List<Position> plusOneOrLessR4rPositions = r4rPositions.stream()
                    .filter(p -> p.getStreak() <= 1)
                    .collect(Collectors.toList());
            if (!plusOneOrLessR4rPositions.isEmpty()) {
                r4rPositions = plusOneOrLessR4rPositions;
            }
        }
        if (r4rPositions.isEmpty()) {
            return null;
        }
        int randomInt = random.nextInt(100);
        if (randomInt < 25) {
            // TODO improve filter: really want recent errors, this session or last session for example, i.e. negative streak
            // LIFO
            return r4rPositions.stream()
                    // .max(Comparator.comparingLong(Position::getReadyForReviewTimeMillis))
                    .max(Comparator.comparingLong(Position::getLastImpressionTimeMillis))
                    .get();
        }
        if (randomInt < 50) {
            Position weakest = r4rPositions.stream()
                    .min(Comparator.comparingInt(Position::getStreak))
                    .get();
            // what about weakest by least impressions, too, or least percent of success?
            // what about weakest with single impression, -1 streak?
            if (weakest.getStreak() < 0) {
                return weakest;
            }
            // Random
            // TODO prefer streaks < 4
            return r4rPositions.get(random.nextInt(r4rPositions.size()));
        }
        // remaining 50%
        // TODO consider grouping positions by (positive) streak
        // FIFO
        // TODO prefer streaks < 4
        return r4rPositions.stream()
                .min(Comparator.comparingLong(Position::getReadyForReviewTimeMillis))
                .get();
    }

    private boolean isFiat(boolean hasUnknownSentences, Integer sessionFiatPercent) {
        if (dictationProperties.getPrefix() != null) {
            return false;
        }
        if (!hasUnknownSentences) {
            return false;
        }
        return random.nextInt(100) < sessionFiatPercent;
    }

    @PostMapping
    String submitTranscript(@RequestParam String language,
                            @RequestParam(required = false) String corpus,
                            @RequestParam String fileName,
                            @RequestParam String userTranscript,
                            @RequestParam Long sessionId,
                            @RequestParam Integer sessionFiatPercent,
                            @RequestParam(required = false) boolean translate,
                            @RequestParam(required = false) boolean reserve,
                            @RequestParam(required = false) boolean draw,
                            @RequestAttribute String userName,
                            @RequestAttribute Long currentTimeMillis,
                            RedirectAttributes redirectAttributes,
                            // TODO add optional volume level parameter, if volume had to be raised
                            Model model) {
        validateLanguageAndCorpus(language, corpus);
        validateSessionFiatPercent(sessionFiatPercent);
        Sentence sentence = sentenceRepository.findByFileName(fileName)
                .orElseThrow(() -> new RuntimeException("no sentence found for " + fileName));
        if (!sentence.getLanguage().equals(language)) {
            throw new RuntimeException("unexpected language for file name");
        }
        Transaction transaction = new Transaction();
        transaction.setUserName(userName);
        transaction.setLanguage(language);
        transaction.setCorpus(sentence.getCorpus());
        transaction.setFileName(fileName);
        transaction.setUserTranscript(userTranscript.trim());
        transaction.setSessionId(sessionId);
        transaction.setTimeMillis(currentTimeMillis);
        if (reserve) {
            transaction.setReserved(true);
        }
        String prettyHtmlDiff = diffMatchPatchHelper.diff(sentence, userTranscript);
        transaction.setCorrect(prettyHtmlDiff == null);
        saveTransactionEtc(transaction);
        if (transaction.getCorrect() && !translate && !draw) {
            // TODO consider if first correct for position to show translation
            // TODO consider error severity: INFO, WARN, ERROR, FATAL
            // only on warn, whatever the criteria, do you have a choice
            // what levenshtein distance thresholds to specify? or make it configurable?
            setSessionRedirectAttributes(language, corpus, sessionId, sessionFiatPercent, redirectAttributes);
            // TODO add prevTimeMillis to redirect for debugging information,
            //  to make it easier to find individual transactions
            return "redirect:/dictation";
        }
        boolean showTranslation;
        if (translate) {
            showTranslation = true;
        } else {
            showTranslation = transactionRepository.countByUserNameAndLanguage(userName, language) > 40000;
        }
        putTransactionInModel(transaction, model, showTranslation);
        if (draw) {
            String imageLink = imageService.generateImage(sentence.getTranscript());
            if (imageLink != null) {
                model.addAttribute("imageLink", imageLink);
            }
        }
        Model primaryAttributes = getPrimaryAttributes(language, corpus, sessionId, sessionFiatPercent);
        model.addAttribute("primaryAttributes", primaryAttributes.asMap().entrySet());
        if (transaction.getCorrect() && (translate || draw)) {
            return "translate";
        }
        model.addAttribute("showTranslation", translate);
        return "diff";
    }

    private static void setSessionRedirectAttributes(String language, String corpus, Long sessionId, Integer sessionFiatPercent, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("language", language);
        redirectAttributes.addAttribute("corpus", corpus);
        redirectAttributes.addAttribute("sessionId", sessionId);
        redirectAttributes.addAttribute("sessionFiatPercent", sessionFiatPercent);
    }

    private Model getPrimaryAttributes(String language, String corpus, Long sessionId, Integer sessionFiatPercent) {
        Model primaryAttributes = new ExtendedModelMap();
        primaryAttributes.addAttribute("language", language);
        primaryAttributes.addAttribute("corpus", corpus);
        primaryAttributes.addAttribute("sessionId", sessionId);
        primaryAttributes.addAttribute("sessionFiatPercent", sessionFiatPercent);
        return primaryAttributes;
    }

    private void saveTransactionEtc(Transaction transaction) {
        transactionRepository.save(transaction);
        applicationEventPublisher.publishEvent(new TransactionSavedEvent(transaction, this));
    }

    @GetMapping("replay")
    String replaySession(@RequestParam String language,
                         @RequestParam Long sessionId,
                         @RequestParam(defaultValue = "0") Long prevTimeMillis,
                         @RequestParam(required = false) Boolean errorsOnly,
                         @RequestAttribute String userName,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        // TODO add search parameter to find words in individual sentences
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndSessionId(userName, sessionId);
        Optional<Transaction> optionalTransaction = transactions.stream()
                .filter(t -> t.getTimeMillis() > prevTimeMillis)
                .filter(t -> {
                    if ((errorsOnly != null) && errorsOnly) {
                        return !t.getCorrect();
                    }
                    return true;
                })
                .min(Comparator.comparing(Transaction::getTimeMillis));
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            putTransactionInModel(transaction, model, true);
            Model primaryAttributes = new ExtendedModelMap();
            primaryAttributes.addAttribute("language", language);
            primaryAttributes.addAttribute("sessionId", sessionId);
            primaryAttributes.addAttribute("prevTimeMillis", transaction.getTimeMillis());
            model.addAttribute("primaryAttributes", primaryAttributes.asMap().entrySet());
            model.addAttribute("errorsOnly", errorsOnly);
            return "replay";
        }
        redirectAttributes.addAttribute("language", language);
        return "redirect:/sessions";
    }

    private void putTransactionInModel(Transaction transaction, Model model, boolean showTranslation) {
        String fileName = transaction.getFileName();
        // TODO consider a transaction namespace in model, rather than flat bag of properties
        model.addAttribute("language", transaction.getLanguage());
        model.addAttribute("corpus", transaction.getCorpus());
        model.addAttribute("fileName", fileName);
        model.addAttribute("transactionCorrect", transaction.getCorrect());
        model.addAttribute("transactionForceCorrect",
                transaction.getForceCorrect() != null && transaction.getForceCorrect());
        model.addAttribute("transactionReserved",
                transaction.getReserved() != null && transaction.getReserved());
        model.addAttribute("audioHeaderAndPayload",
                audioService.getAudioHeaderAndPayload(transaction.getCorpus(), fileName));
        Sentence sentence = sentenceRepository.findByFileName(fileName)
                .orElseThrow(() -> new RuntimeException("no sentence found for " + fileName));
        String prettyHtmlDiff = diffMatchPatchHelper.diff(sentence, transaction.getUserTranscript());
        model.addAttribute("prettyHtmlDiff", prettyHtmlDiff);
        model.addAttribute("transcript", sentence.getTranscript());
        model.addAttribute("userTranscript", transaction.getUserTranscript());
        if (showTranslation) {
            String translation = translationService.translate(transaction.getLanguage(), sentence.getTranscript());
            model.addAttribute("translation", translation);
        }
        // TODO get/generate audio of translation (via Polly or Google), add as audio element to translation div
        model.addAttribute("timeMillis", transaction.getTimeMillis());
        model.addAttribute("isReserved", (transaction.getReserved() != null) && transaction.getReserved());
    }

    /**
     * Intended to be called from "diff", "translate", other templates after "prepare".
     * An acknowledgement, from the diff for example, could reserve the position,
     * but also forcibly mark the transaction as correct.
     * User could also ask for a drawing (DALLE), which for now, could lead to a separate screen,
     * as an alternative to lazily loading the drawing, via a Javascript fetch call, once a button (on diff)
     * is clicked.
     */
    @PostMapping("acknowledge")
    String acknowledge(@RequestParam String language,
                       @RequestParam(required = false) String corpus,
                       @RequestParam Long sessionId,
                       @RequestParam Long timeMillis,
                       @RequestParam Integer sessionFiatPercent,
                       @RequestParam(required = false) boolean correct,
                       @RequestParam(required = false) boolean reserve,
                       @RequestParam(required = false) boolean release,
                       @RequestAttribute String userName,
                       RedirectAttributes redirectAttributes) {
        if (correct || reserve || release) {
            Transaction transaction = getTransaction(language, corpus, sessionId, timeMillis, userName);
            if (correct) {
                if (transaction.getCorrect()) {
                    throw new RuntimeException("unexpected correct transaction");
                }
                transaction.setForceCorrect(true);
                transaction.setCorrect(true);
            }
            if (reserve) {
                transaction.setReserved(true);
            } else if (release) {
                transaction.setReserved(false);
            }
            saveTransactionEtc(transaction);
        }
        setSessionRedirectAttributes(language, corpus, sessionId, sessionFiatPercent, redirectAttributes);
        // TODO add "playoff" acknowledgement, when you will force correct a position for second time in a row:
        //  you just have to copy the official transcript, to build the muscle memory. But with 15000 sentences remaining...
        return "redirect:/dictation";
    }

    /**
     * Override incorrect score, in case
     *  1) You misspelled a foreign word, such as a proper name in Italian pronounced by a German speaker
     *  2) You wrote a contraction when two syllables were pronounced separately but barely audible
     */
    @PostMapping("correct")
    String correct(@RequestParam String language,
                   @RequestParam(required = false) String corpus,
                   @RequestParam Long sessionId,
                   @RequestParam Long timeMillis,
                   @RequestParam Integer sessionFiatPercent,
                   @RequestAttribute String userName,
                   RedirectAttributes redirectAttributes) {
        Transaction transaction = getTransaction(language, corpus, sessionId, timeMillis, userName);
        if (transaction.getCorrect()) {
            throw new RuntimeException("unexpected correct transaction");
        }
        transaction.setForceCorrect(true);
        transaction.setCorrect(true);
        saveTransactionEtc(transaction);
        setSessionRedirectAttributes(language, corpus, sessionId, sessionFiatPercent, redirectAttributes);
        return "redirect:/dictation";
    }

    @PostMapping("reserve")
    String reserve(@RequestParam String language,
                   @RequestParam(required = false) String corpus,
                   @RequestParam Long sessionId,
                   @RequestParam Long timeMillis,
                   @RequestParam Integer sessionFiatPercent,
                   @RequestAttribute String userName,
                   RedirectAttributes redirectAttributes) {
        Transaction transaction = getTransaction(language, corpus, sessionId, timeMillis, userName);
        transaction.setReserved(true);
        saveTransactionEtc(transaction);
        setSessionRedirectAttributes(language, corpus, sessionId, sessionFiatPercent, redirectAttributes);
        return "redirect:/dictation";
    }

    @PostMapping("release")
    String release(@RequestParam String language,
                   @RequestParam(required = false) String corpus,
                   @RequestParam Long sessionId,
                   @RequestParam Long timeMillis,
                   @RequestAttribute String userName,
                   RedirectAttributes redirectAttributes) {
        Transaction transaction = getTransaction(language, corpus, sessionId, timeMillis, userName);
        if (transaction.getReserved() != null && !transaction.getReserved()) {
            throw new RuntimeException("unexpected unreserved transaction");
        }
        transaction.setReserved(false);
        saveTransactionEtc(transaction);
        return "redirect:/dictation";
    }

    private Transaction getTransaction(String language, String corpus, Long sessionId, Long timeMillis, String userName) {
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndSessionId(userName, sessionId);
        Transaction transaction = transactions.stream()
                .filter(t -> t.getTimeMillis().equals(timeMillis))
                .filter(t -> t.getLanguage().equals(language))
                .filter(t -> corpus == null || t.getCorpus().equals(corpus))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to find transaction for timeMillis " + timeMillis));
        return transaction;
    }

    // TODO add copy API if negative streak severe
}
