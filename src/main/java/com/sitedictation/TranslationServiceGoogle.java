package com.sitedictation;

import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(prefix = "dictation", value = "translation-enabled", havingValue = "true")
@Slf4j
class TranslationServiceGoogle implements TranslationService {

    private final Translate translate;
    private final Map<String, String> languageNameToCode = new HashMap<>();
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    TranslationServiceGoogle(Translate translate) {
        this.translate = translate;
    }

    @PostConstruct
    private void init() {
        List<Language> gLanguages = translate.listSupportedLanguages();
        for (Language gLanguage : gLanguages) {
            languageNameToCode.put(gLanguage.getName().toLowerCase(), gLanguage.getCode());
        }
    }

    @Override
    public String translate(String language, String transcript) {
        Map<String, String> cacheForLanguage = cache.computeIfAbsent(language, k -> new ConcurrentHashMap<>());
        if (cacheForLanguage.containsKey(transcript)) {
            return cacheForLanguage.get(transcript);
        }
        if (cacheForLanguage.size() > 1000) {
            cacheForLanguage.clear();
        }
        String sourceLanguageCode = languageNameToCode.get(language);
        if (sourceLanguageCode.equalsIgnoreCase("en")) {
            return null;
        }
        Translation translation;
        try {
            translation = getTranslationInternal(transcript, sourceLanguageCode);
        } catch (Exception e) {
            log.error("google translate failed", e);
            return null;
        }
        String translatedText = translation.getTranslatedText();
        cacheForLanguage.put(transcript, translatedText);
        return translatedText;
    }

    private Translation getTranslationInternal(String transcript, String sourceLanguageCode) throws TimeoutException, ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Translation> future = executor.submit(() -> translate.translate(
                transcript,
                Translate.TranslateOption.sourceLanguage(sourceLanguageCode),
                Translate.TranslateOption.targetLanguage("en"),
                Translate.TranslateOption.format("text")));
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }
}
