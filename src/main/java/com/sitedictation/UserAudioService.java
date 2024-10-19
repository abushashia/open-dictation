package com.sitedictation;

import java.util.List;

interface UserAudioService {

    void saveUserAudio(String originalFileName, String base64Payload, Long currentTimeMillis);

    List<String> getAudioHeadersAndPayloadList(String originaFileName);

    int size();
}
