package com.sitedictation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(indexes = {
        @Index(columnList = "userName, corpus"),
        @Index(columnList = "userName, language"),
        @Index(columnList = "sessionId"),
})
class Transaction {

    @GeneratedValue
    @Id
    private Long id;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String corpus;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long timeMillis;

    @Column(nullable = false, length = 2048)
    private String userTranscript;

    @Column(nullable = false)
    private Boolean correct;

    @Column(nullable = true)
    private Boolean forceCorrect;

    @Column(nullable = true)
    private Boolean reserved;

    @Transient
    LocalDateTime getLocalDateTime() {
        Instant instant = Instant.ofEpochMilli(timeMillis);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    @Transient
    LocalDate getLocalDate() {
        return getLocalDateTime().toLocalDate();
    }

    @Transient
    YearMonth getYearMonth() {
        LocalDate localDate = getLocalDate();
        return YearMonth.of(localDate.getYear(), localDate.getMonth());
    }
}
