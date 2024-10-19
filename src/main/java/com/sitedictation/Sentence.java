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

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(indexes = {
        @Index(columnList = "language"),
        @Index(columnList = "corpus"),
        @Index(columnList = "fileName", unique = true),
})
class Sentence {

    @GeneratedValue
    @Id
    private Long id;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String corpus;

    @Column(nullable = false, unique = true)
    private String fileName;

    @Column(nullable = false, length = 2048)
    private String transcript;
}
