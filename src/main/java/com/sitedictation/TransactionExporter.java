package com.sitedictation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
class TransactionExporter {

    private final ObjectMapper objectMapper;
    private final TransactionExporterHelper transactionExporterHelper;
    private final TransactionRepository transactionRepository;

    TransactionExporter(ObjectMapper objectMapper,
                        TransactionExporterHelper transactionExporterHelper,
                        TransactionRepository transactionRepository) {
        this.objectMapper = objectMapper;
        this.transactionExporterHelper = transactionExporterHelper;
        this.transactionRepository = transactionRepository;
    }

    @Async
    @EventListener
    void handleTransactionSavedEvent(TransactionSavedEvent event) {
        Transaction transaction = event.getTransaction();
        log.info("Saving transaction for {}, correct = {}", transaction.getFileName(), transaction.getCorrect());
        if (Objects.equals(transaction.getForceCorrect(), true)) {
            log.info("Forcing transaction for {} to be correct", transaction.getFileName());
        }
        if (Objects.equals(transaction.getReserved(), true)) {
            log.info("Reserving {}", transaction.getFileName());
        }
        String userName = transaction.getUserName();
        String corpus = transaction.getCorpus();
        List<Transaction> transactions = transactionRepository
                .findAllByUserNameAndCorpus(userName, corpus);
        transactions.parallelStream().forEach(txn -> txn.setId(null));
        // put most recent at the top for debugging
        transactions.sort(Comparator.comparing(Transaction::getTimeMillis).reversed());
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(transactions);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // either s3, as in prod, or local file
        transactionExporterHelper.exportTransactions("transactions-" + userName + "-" + corpus + ".json", bytes);
        log.info(String.format("Saved %d transactions for corpus %s to %s",
                transactions.size(), transactions.get(0).getCorpus(), "transactions-" + userName + "-" + corpus + ".json"));
    }
}
