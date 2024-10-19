package com.sitedictation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface SentenceRepository extends JpaRepository<Sentence, Long> {

    Optional<Sentence> findByFileName(String fileName);

    long countByLanguage(String language);

    long countByCorpus(String corpus);

    long countByLanguageAndCorpus(String language, String corpus);

    List<Sentence> findAllByLanguage(String language);

    List<Sentence> findAllByLanguageAndCorpus(String language, String corpus);

    @Query("SELECT DISTINCT s.language FROM Sentence s ORDER BY s.language")
    List<String> findDistinctLanguages();

    @Query("SELECT DISTINCT s.corpus FROM Sentence s WHERE s.language = ?1 ORDER BY s.corpus")
    List<String> findDistinctCorpora(String language);
}
