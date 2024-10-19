package com.sitedictation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Controller
@RequestMapping("sessions")
class SessionsController {

    private final SentenceRepository sentenceRepository;
    private final TransactionRepository transactionRepository;

    SessionsController(SentenceRepository sentenceRepository, TransactionRepository transactionRepository) {
        this.sentenceRepository = sentenceRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    String getSessions(@RequestParam String language, @RequestAttribute String userName, Model model) {
        List<Transaction> transactions = transactionRepository.findAllByUserNameAndLanguage(userName, language);
        transactions.sort(Comparator.comparing(Transaction::getTimeMillis));
        Set<String> fileNames = new HashSet<>();
        Set<String> transcripts = new HashSet<>();
        Map<Long, Set<String>> fileNamesBySessionId = new HashMap<>();
        Map<Long, Session> sessionsBySessionId = new TreeMap<>(Comparator.reverseOrder());
        Set<Long> transactionTimeMillis = new HashSet<>();
        for (Transaction transaction : transactions) {
            if (!transactionTimeMillis.add(transaction.getTimeMillis())) {
                throw new RuntimeException("more than one transaction for timeMillis " + transaction.getTimeMillis());
            }
            Session session = sessionsBySessionId.computeIfAbsent(transaction.getSessionId(), Session::new);
            session.incrementTransactionsCount();
            Set<String> fileNamesForSession = fileNamesBySessionId
                    .computeIfAbsent(transaction.getSessionId(), k -> new HashSet<>());
            String fileName = transaction.getFileName();
            if (fileNamesForSession.add(fileName)) {
                session.incrementPositionsCount();
            }
            if (transaction.getCorrect()) {
                session.incrementSuccessesCount();
            }
            if (fileNames.add(fileName)) {
                session.incrementNewPositionsCount();
                if (transaction.getCorrect()) {
                    session.incrementNewSuccessesCount();
                }
                Sentence sentence = sentenceRepository.findByFileName(fileName)
                        .orElseThrow(() -> new RuntimeException("no sentence for " + fileName));
                if (transcripts.add(sentence.getTranscript())) {
                    session.incrementNewTranscriptsCount();
                    if (transaction.getCorrect()) {
                        session.incrementNewTranscriptSuccessesCount();
                    }
                }
            }
            if ((transaction.getForceCorrect() != null) && transaction.getForceCorrect()) {
                session.setForceCorrectCount(1 + session.getForceCorrectCount());
            }
            if ((transaction.getReserved() != null) && transaction.getReserved()) {
                session.setReservationsCount(1 + session.getReservationsCount());
            }
        }
        model.addAttribute("language", language);
        model.addAttribute("sessions", sessionsBySessionId.values());
        return "sessions";
    }
}
