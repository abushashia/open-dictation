package com.sitedictation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
class HomeController {

    private final DictationProperties dictationProperties;
    private final PerformanceService performanceService;
    private final PositionService positionService;
    private final SentenceRepository sentenceRepository;
    private final UserAudioService userAudioService;

    HomeController(DictationProperties dictationProperties,
                   PerformanceService performanceService,
                   PositionService positionService,
                   SentenceRepository sentenceRepository,
                   UserAudioService userAudioService) {
        this.dictationProperties = dictationProperties;
        this.performanceService = performanceService;
        this.positionService = positionService;
        this.sentenceRepository = sentenceRepository;
        this.userAudioService = userAudioService;
    }

    @GetMapping
    String home(@RequestAttribute String userName, @RequestAttribute Long currentTimeMillis, Model model) {
        model.addAttribute("userName", userName);
        model.addAttribute("prefix", dictationProperties.getPrefix());
        List<UserLanguageData> userLanguageDataList = new ArrayList<>();
        model.addAttribute("userLanguageDataList", userLanguageDataList);
        List<String> distinctLanguages = sentenceRepository.findDistinctLanguages();
        for (String language : distinctLanguages) {
            UserLanguageData userLanguageData = new UserLanguageData();
            userLanguageDataList.add(userLanguageData);
            userLanguageData.setLanguage(language);
            userLanguageData.setLanguageDisplayName(StringUtils.capitalize(language));
            userLanguageData.setSentences(sentenceRepository.countByLanguage(language));
            List<Position> positions = positionService.getPositions(userName, language);
            userLanguageData.setPositions(positions.size());
            userLanguageData.setR4r(positions.stream()
                    .filter(p -> p.isReadyForReview(currentTimeMillis))
                    .count());
            userLanguageData.setTransactions(positions.stream()
                    .mapToInt(Position::getTransactionsCount)
                    .sum());
            Set<Long> sessionIds = new HashSet<>();
            for (Position position : positions) {
                sessionIds.addAll(position.getSessionIds());
            }
            if (!sessionIds.isEmpty()) {
                userLanguageData.setSessions(sessionIds.size());
            }
            PerformanceData perfData = performanceService.getCumulativePerformance(userName, language);
            userLanguageData.setSuccess(perfData.getSuccessRate());
            // TODO consider username and language in user audio file names
            userLanguageData.setUserAudioFiles(userAudioService.size());
        }
        userLanguageDataList.sort(Comparator.comparingInt(UserLanguageData::getPositions).reversed());
        model.addAttribute("userAudioEnabled", dictationProperties.isUserAudioEnabled());
        return "home";
    }

    @GetMapping("health")
    String health() {
        return "redirect:/actuator/health";
    }

    @GetMapping("info")
    String info() {
        return "redirect:/actuator/info";
    }

    @GetMapping("metrics")
    String metrics() {
        return "redirect:/actuator/metrics";
    }

    @GetMapping("prometheus")
    String prometheus() {
        return "redirect:/actuator/prometheus";
    }

    @GetMapping("terms")
    String terms() {
        return "terms";
    }

    @GetMapping("privacy")
    String privacy() {
        return "privacy";
    }
}
