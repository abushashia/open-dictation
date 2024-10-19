package com.sitedictation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "dictation", value = "user-audio-enabled", havingValue = "false")
class UserAudioServiceNoOp implements UserAudioService {

    @Override
    public void saveUserAudio(String originalFileName, String base64Payload, Long currentTimeMillis) {
        // empty block
    }

    @Override
    public List<String> getAudioHeadersAndPayloadList(String originaFileName) {
        return Collections.emptyList();
    }

    @Override
    public int size() {
        return 0;
    }
}
