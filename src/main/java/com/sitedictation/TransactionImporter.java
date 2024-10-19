package com.sitedictation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
class TransactionImporter {

    private final CorpusMetadataHelper corpusMetadataHelper;
    private final DictationProperties dictationProperties;
    private final ObjectMapper objectMapper;
    private final TransactionImporterHelper transactionImporterHelper;
    private final TransactionRepository transactionRepository;

    private final Set<String> TOO_LOUD = new HashSet<>(Arrays.asList(
            "cf9a946a9e1492b3a3cfca50b257d37ea30c23042f7d8e6ba28d7350a8b6c143b5689859ab0fdb639f44dce49da8eff8cd3c57b6a3ebd873f9567a2948da025d.mp3",
            "70256c365b0f4ad539e5805a7993001447026914aca32b504e8fc57830f0ec681d7c63717eba2a6d2a09ddef3b514c10ea0e3cab1f8758e4e1e5c7db78ba3317.mp3",
            "6fa2557f31d5dc5cb49e8f8479e348808fbc0c196681d0712eb8617bf064b173d6e093797aa0a4e414e358c08c4b4e52b3ed6d9b4fc8978ccb7d69261b33be0d.mp3",
            "fee1e0431b58f751c5b07fdda8049c6f755072cfe234e59368ee0fb96779840a5b0c332d5a6d4b62f1af9aedc6dde6814d0c7d32841e2920538d6d56c1b60d2d.mp3",
            "14fd26619da2c99be7cb6a72abb7ab2051503e4f7214ca4948f1e739f03d2e074bd8c1edb6d8b91984fb2812cb9dc1c4d7e6f2a0e12f03f538a5beb022353da4.mp3",
            "5123d0df23fbfa9f12c987c4f6d5c28360db0cf79593662a5cd491d737059a1480d0b9e4b71f975a145eeaa0b07f0def7c49e15421b762c94f81ccde425892d0.mp3",
            "9ac0c4d9654b3d14f8578c9f73ec3e2b1b81c77f643a318bc77844b23ea2557d84f7a65e4c0e58e8079d9a510ed445c8a9dc570f8cb8812ffcac28644f163f79.mp3"
    ));

    TransactionImporter(CorpusMetadataHelper corpusMetadataHelper,
                        DictationProperties dictationProperties,
                        ObjectMapper objectMapper,
                        TransactionImporterHelper transactionImporterHelper,
                        TransactionRepository transactionRepository) {
        this.corpusMetadataHelper = corpusMetadataHelper;
        this.dictationProperties = dictationProperties;
        this.objectMapper = objectMapper;
        this.transactionImporterHelper = transactionImporterHelper;
        this.transactionRepository = transactionRepository;
    }

    /**
     * As an alternative to importing during context initialization,
     * don't use Repository, instead Cache, initialize Cache only when needed,
     * so that this Site may host multiple games that use SRS, fiat factor, etc.
     */
    @PostConstruct
    private void importTransactions() throws IOException {
        for (String fileName : transactionImporterHelper.getFileNames()) {
            importTransactions(fileName, transactionImporterHelper.getInputStream(fileName));
        }
    }

    private void importTransactions(String transactionsFileName, InputStream inputStream) throws IOException {
        List<Transaction> transactions = objectMapper.readValue(inputStream, new TypeReference<List<Transaction>>() {});
        if (dictationProperties.getFocusLanguage() != null) {
            String language = transactions.get(0).getLanguage();
            if (!language.equalsIgnoreCase(dictationProperties.getFocusLanguage())) {
                return;
            }
        }
        String corpus = transactions.get(0).getCorpus();
        if (!corpusMetadataHelper.hasCorpus(corpus)) {
            return;
        }
        transactions.forEach(t -> t.setId(null));
        Set<Long> transactionTimeMillis = new HashSet<>();
        for (Iterator<Transaction> iterator = transactions.iterator(); iterator.hasNext(); ) {
            Transaction transaction = iterator.next();
            if (!transactionTimeMillis.add(transaction.getTimeMillis())) {
                throw new RuntimeException("too many transactions for timeMillis " + transaction.getTimeMillis());
            }
            if (transaction.getLanguage() == null) {
                transaction.setLanguage(corpusMetadataHelper.getLanguageForCorpus(transaction.getCorpus()));
            }
            if (!transaction.getFileName().contains(".")) {
                transaction.setFileName(transaction.getFileName() + ".mp3");
            }
            if (StringUtils.isNotBlank(dictationProperties.getFocusLanguage())) {
                if (dictationProperties.getFocusLanguage().equalsIgnoreCase("german")) {
                    if (TOO_LOUD.contains(transaction.getFileName())) {
                        log.info("Removing transaction for {} at {} because audio too loud",
                                transaction.getFileName(), transaction.getTimeMillis());
                        iterator.remove();
                    }
                }
            }
        }
        Lists.partition(transactions, 10000)
                .parallelStream()
                .forEach(transactionRepository::saveAll);
        log.info(String.format("Saved %d transactions from %s", transactions.size(), transactionsFileName));
    }
}
