package com.sitedictation;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
class DictationInfoContributor implements InfoContributor {

    private final DictationProperties dictationProperties;
    private final TransactionRepository transactionRepository;

    DictationInfoContributor(DictationProperties dictationProperties,
                             TransactionRepository transactionRepository) {
        this.dictationProperties = dictationProperties;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("distinctUserNames", transactionRepository.findDistinctUserNames().size());
        Optional<Transaction> optionalLastTransaction = transactionRepository.findFirstByOrderByTimeMillisDesc();
        if (optionalLastTransaction.isPresent()) {
            builder.withDetail("lastTxnLocalDateTime", optionalLastTransaction.get().getLocalDateTime());
        }

        List<Long> distinctSessions = transactionRepository.findDistinctSessionIds();
        long twentyFiveMinutesAgo = Duration.ofMillis(System.currentTimeMillis())
                .minus(dictationProperties.getSessionDuration())
                .toMillis();
        long activeSessions = distinctSessions.stream().filter(s -> s > twentyFiveMinutesAgo).count();
        builder.withDetail("activeSessions", activeSessions);
    }
}