package com.sitedictation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByUserNameAndSessionId(String userName, Long sessionId);

    List<Transaction> findAllByUserNameAndCorpus(String userName, String corpus);

    List<Transaction> findAllByUserNameAndLanguage(String userName, String language);

    List<Transaction> findAllByUserNameAndLanguageAndFileName(String userName, String language, String fileName);

    long countByUserNameAndLanguage(String userName, String language);

    List<Transaction> findAllByUserNameAndLanguageAndCorpus(String userName, String language, String corpus);

    List<Transaction> findAllByUserName(String userName);

    @Query("SELECT DISTINCT t.sessionId FROM Transaction t WHERE t.userName = ?1 ORDER BY t.sessionId")
    List<Long> findDistinctSessionIds(String userName);

    @Query("SELECT DISTINCT t.sessionId FROM Transaction t ORDER BY t.sessionId")
    List<Long> findDistinctSessionIds();

    @Query("SELECT DISTINCT t.userName FROM Transaction t ORDER BY t.userName")
    List<String> findDistinctUserNames();

    Optional<Transaction> findFirstByOrderByTimeMillisDesc();
}
