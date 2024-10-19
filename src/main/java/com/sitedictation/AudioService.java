package com.sitedictation;

import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
class AudioService {

    // TODO use Guava CacheBuilder or w/e Spring recommends to cap cache size
    private final ConcurrentMap<String, String> audioPayloadCache = new ConcurrentHashMap<>();
    private final AudioServiceHelper audioServiceHelper;

    AudioService(AudioServiceHelper audioServiceHelper) {
        this.audioServiceHelper = audioServiceHelper;
    }

    /**
     * For example, data:audio/mpeg;base64,/+MYxAAEaAIEeUAQAgBgNgP//
     */
    public String getAudioHeaderAndPayload(String corpus, String fileName) {
        return getAudioHeader(fileName) + getAudioPayload(corpus, fileName);
    }

    private String getAudioPayload(String corpus, String fileName) {
        if (audioPayloadCache.containsKey(fileName)) {
            return audioPayloadCache.get(fileName);
        }
        if (audioPayloadCache.size() > 1000) {
            audioPayloadCache.clear();
        }
        byte[] bytes = audioServiceHelper.getAudioPayload(corpus, fileName);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        audioPayloadCache.put(fileName, base64);
        return base64;
    }

    private String getAudioHeader(String fileName) {
        if (fileName.endsWith(".mp3")) {
            return "data:audio/mpeg;base64,";
        }
        if (fileName.endsWith(".m4a")) {
            return "data:audio/mpeg;base64,";
        }
        if (fileName.endsWith(".wav")) {
            return "data:audio/wav;base64,";
        }
        throw new IllegalArgumentException("unexpected file extension for file " + fileName);
    }
}
