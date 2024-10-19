package com.sitedictation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.YearMonth;

@AllArgsConstructor
@Data
class WordDataMonthly {

    private YearMonth yearMonth;
    private int newWords;
    private int cumWords;
}
