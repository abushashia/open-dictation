package com.sitedictation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Controller
@RequestMapping("performance")
class PerformanceController {

    private final PerformanceService performanceService;

    PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping
    String getPerformance(@RequestParam String language,
                          @RequestParam(required = false) YearMonth yearMonth,
                          @RequestAttribute String userName,
                          Model model) {
        SortedMap<YearMonth, PerformanceData> performance = performanceService.getPerformance(userName, language);
        SortedMap<YearMonth, PerformanceData> performanceDesc = new TreeMap<>(Comparator.reverseOrder());
        if (yearMonth != null) {
            for (Map.Entry<YearMonth, PerformanceData> entry : performance.entrySet()) {
                if (entry.getKey().equals(yearMonth)) {
                    performanceDesc.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            performanceDesc.putAll(performance);
        }
        model.addAttribute("perfDatas", performanceDesc.values());
        model.addAttribute("language", language);
        if (yearMonth != null) {
            return "performance-details";
        }
        return "performance";
    }
}
