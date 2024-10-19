package com.sitedictation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties
class CorpusMetadataHelper {

    private static final List<String> RTL_LANGUAGES = Arrays.asList(
            "arabic", "armenian", "farsi", "hebrew", "persian", "urdu");

    private List<Corpus> corpora;

    @Autowired
    private CommonVoiceProperties commonVoiceProperties;

    private final Map<String, String> corpusToLanguage = new HashMap<>();
    private final Map<String, String> corpusToBucket = new HashMap<>();
    private final Set<String> languages = new HashSet<>();

    @PostConstruct
    private void init() {
        if (commonVoiceProperties.getLanguage() != null) {
            Corpus corpus = new Corpus();
            corpus.setName("validated.tsv");
            corpus.setLanguage(commonVoiceProperties.getLanguage());
            corpus.setBucket(commonVoiceProperties.getDirectory() + "clips/");
            corpora = Collections.singletonList(corpus);
        }
        for (Corpus corpus : corpora) {
            corpusToLanguage.put(corpus.getName(), corpus.getLanguage());
            corpusToBucket.put(corpus.getName(), corpus.getBucket());
            languages.add(corpus.getLanguage());
        }
    }

    public List<Corpus> getCorpora() {
        return corpora;
    }

    public void setCorpora(List<Corpus> corpora) {
        this.corpora = corpora;
    }

    String getLanguageForCorpus(String corpus) {
        return corpusToLanguage.get(corpus);
    }

    String getBucketForCorpus(String corpus) {
        if (!corpusToBucket.containsKey(corpus)) {
            throw new RuntimeException("unexpected corpus for mapping to bucket: " +  corpus);
        }
        return corpusToBucket.get(corpus);
    }

    Set<String> getLanguages() {
        return languages;
    }

    boolean isStripAccents(String language) {
        return !language.equalsIgnoreCase("romanian");
    }

    boolean isIgnoreWhitespaceDiffs(String language) {
        return !language.equalsIgnoreCase("romanian");
    }

    boolean isRightToLeft(String language) {
        return RTL_LANGUAGES.contains(language);
    }

    boolean hasCorpus(String corpus) {
        return corpusToLanguage.containsKey(corpus);
    }
}
