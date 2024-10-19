package com.sitedictation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@ConditionalOnProperty(prefix = "dictation", value = "user-audio-enabled", havingValue = "true")
@RestController
@RequestMapping("useraudio")
@Slf4j
class UserAudioRestController {

    private final UserAudioService userAudioService;

    UserAudioRestController(UserAudioService userAudioService) {
        this.userAudioService = userAudioService;
    }

    @GetMapping
    PayloadList getPayloads(@RequestParam String originalFileName) {
        List<String> audioHeadersAndPayloadList = userAudioService.getAudioHeadersAndPayloadList(originalFileName);
        // TODO just return a list
        return new PayloadList(audioHeadersAndPayloadList);
    }

    @PostMapping
    ResponseEntity<Void> postMessage(@RequestBody Message message, @RequestAttribute Long currentTimeMillis) {
        userAudioService.saveUserAudio(message.getOriginalFileName(), message.getUserAudioPayload(), currentTimeMillis);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Getter
    private static class Message {
        private String originalFileName;
        private String userAudioPayload;
    }

    @Getter
    private static class PayloadList {

        private final Collection<String> payloads;

        private PayloadList(Collection<String> payloads) {
            this.payloads = payloads;
        }
    }
}
