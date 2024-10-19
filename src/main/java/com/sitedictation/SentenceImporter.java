package com.sitedictation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Component
@Slf4j
class SentenceImporter {

    private final CorpusMetadataHelper corpusMetadataHelper;
    private final DictationProperties dictationProperties;
    private final SentenceImporterHelper sentenceImporterHelper;
    private final SentenceRepository sentenceRepository;

    SentenceImporter(CorpusMetadataHelper corpusMetadataHelper,
                     DictationProperties dictationProperties,
                     SentenceImporterHelper sentenceImporterHelper,
                     SentenceRepository sentenceRepository) {
        this.corpusMetadataHelper = corpusMetadataHelper;
        this.dictationProperties = dictationProperties;
        this.sentenceImporterHelper = sentenceImporterHelper;
        this.sentenceRepository = sentenceRepository;
    }

    @PostConstruct
    private void importSentences() {
        for (String corpus : sentenceImporterHelper.getCorpora()) {
            String languageForCorpus = corpusMetadataHelper.getLanguageForCorpus(corpus);
            if (dictationProperties.getFocusLanguage() != null) {
                if (languageForCorpus == null) {
                    // because regular german file no longer included in profile properties
                    continue;
                }
                if (!languageForCorpus.equalsIgnoreCase(dictationProperties.getFocusLanguage())) {
                    continue;
                }
            }
            if (!corpusMetadataHelper.hasCorpus(corpus)) {
                continue;
            }
            importCorpus(corpus, sentenceImporterHelper.getInputStream(corpus));
        }
    }

    private void importCorpus(String corpus, InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream)) {
            int indexOfSentencePayload = 0;
            int indexOfAudioFileName = 1;
            List<Sentence> sentences = new ArrayList<>();
            int lineNumber = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNumber++;
                if (dictationProperties.isImportSentencesSlice() && (lineNumber > 100)) {
                    break;
                }
                String[] tokens = line.split("\t");
                if (corpus.equals("validated.tsv") && lineNumber == 1) {
                    for (int index = 0; index < tokens.length; index++) {
                        String token = tokens[index];
                        if (token.equals("sentence")) {
                            indexOfSentencePayload = index;
                        } else if (token.equals("path")) {
                            indexOfAudioFileName = index;
                        }
                    }
                    continue;
                }
                String sentence = tokens[indexOfSentencePayload];
                String sentenceName = tokens[indexOfAudioFileName];
                Sentence sentenceEntity = new Sentence();
                sentenceEntity.setLanguage(corpusMetadataHelper.getLanguageForCorpus(corpus));
                sentenceEntity.setCorpus(corpus);
                sentenceEntity.setFileName(sentenceName);
                sentenceEntity.setTranscript(sentence);
                sentences.add(sentenceEntity);
                if (sentences.size() >= 10000) {
                    sentenceRepository.saveAll(sentences);
                    log.debug(String.format("Saved batch of sentences for %s", corpus));
                    sentences.clear();
                }
            }
            if (!sentences.isEmpty()) {
                sentenceRepository.saveAll(sentences);
                log.debug(String.format("Saved batch of sentences for %s", corpus));
            }
        }
    }
}
