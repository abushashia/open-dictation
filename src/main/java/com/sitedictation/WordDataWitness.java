package com.sitedictation;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
class WordDataWitness {

    private String witness;
    private int wordCount;
    private int wordsForCount;
}
