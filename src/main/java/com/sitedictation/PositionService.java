package com.sitedictation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class PositionService {

    private final TransactionRepository transactionRepository;

    PositionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Returns all positions for userName and language,
     * with no guarantees with respect to their order.
     */
    List<Position> getPositions(String userName, String language) {
        return getPositions(userName, language, null);
    }

    List<Position> getPositions(String userName, String language, String corpus) {
        List<Transaction> transactions;
        if (StringUtils.isNotBlank(corpus)) {
            transactions = transactionRepository.findAllByUserNameAndLanguageAndCorpus(userName, language, corpus);
        } else {
            transactions = transactionRepository.findAllByUserNameAndLanguage(userName, language);
        }
        Map<String, List<Transaction>> groupedByFileName = groupByFileName(transactions);
        return groupedByFileName.values().stream()
                .map(Position::new)
                .collect(Collectors.toList());
    }

    private static Map<String, List<Transaction>> groupByFileName(List<Transaction> transactions) {
        return transactions.stream().collect(Collectors.toMap(
                Transaction::getFileName,
                Collections::singletonList,
                (t1, t2) -> {
                    List<Transaction> merged = new ArrayList<>();
                    merged.addAll(t1);
                    merged.addAll(t2);
                    return merged;
                }));
    }

    Position getPosition(String userName, String language, String fileName) {
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndLanguageAndFileName(userName, language, fileName);
        if (transactions.isEmpty()) {
            return null;
        }
        return new Position(transactions);
    }
}
