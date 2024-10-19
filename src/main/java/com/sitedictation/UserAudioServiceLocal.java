package com.sitedictation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "dictation", value = "user-audio-enabled", havingValue = "true")
@Slf4j
class UserAudioServiceLocal implements UserAudioService {

    private final DictationProperties dictationProperties;
    private final Map<String, SortedSet<UserAudioFileName>> userAudioFileNamesByOriginalFileName = new ConcurrentHashMap<>();
    private final Map<String, File> userAudioFileCache = new ConcurrentHashMap<>();
    private final Map<String, String> userAudioBase64Cache = new ConcurrentHashMap<>();

    UserAudioServiceLocal(DictationProperties dictationProperties) {
        this.dictationProperties = dictationProperties;
    }

    @PostConstruct
    private void init() {
        File directory = new File(dictationProperties.getUserAudioLocalDirectory());
        File[] files = directory.listFiles(f -> f.getName().endsWith(".wav"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String userAudioFileName = file.getName();
            userAudioFileCache.put(userAudioFileName, file);
            addUserAudioFileNameToCache(userAudioFileName);
        }
    }

    private void addUserAudioFileNameToCache(String userAudioFileName) {
        // derive originalFileName (which is used in corpus) from userAudioFileName
        UserAudioFileName userAudioFileNameWrapper = new UserAudioFileName(userAudioFileName);
        // cache userAudioFileNames by originalFileName
        SortedSet<UserAudioFileName> userAudioFilesForFileName = userAudioFileNamesByOriginalFileName
                .computeIfAbsent(userAudioFileNameWrapper.getOriginalFileName(), k -> new TreeSet<>(Comparator.reverseOrder()));
        // sort userAudioFilesForFileNames by timeMillis desc, so that most recent returned first
        userAudioFilesForFileName.add(userAudioFileNameWrapper);
    }

    @Override
    public void saveUserAudio(String originalFileName, String base64Payload, Long currentTimeMillis) {
        String userAudioFileName = originalFileName + "-" + currentTimeMillis + ".wav";
        File userAudioFile = new File(dictationProperties.getUserAudioLocalDirectory() + userAudioFileName);
        byte[] bytes = Base64.getDecoder().decode(base64Payload);
        try {
            FileUtils.writeByteArrayToFile(userAudioFile, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        userAudioFileCache.put(userAudioFileName, userAudioFile);
        userAudioBase64Cache.put(userAudioFileName, base64Payload);
        addUserAudioFileNameToCache(userAudioFileName);
        log.info("Saved {} to {}", userAudioFileName, dictationProperties.getUserAudioLocalDirectory());
    }

    @Override
    public List<String> getAudioHeadersAndPayloadList(String originaFileName) {
        SortedSet<UserAudioFileName> userAudioFileNameWrappers = userAudioFileNamesByOriginalFileName.get(originaFileName);
        if (userAudioFileNameWrappers == null) {
            return Collections.emptyList();
        }
        List<String> audioHeadersAndPayloadList = new ArrayList<>();
        for (UserAudioFileName userAudioFileNameWrapper : userAudioFileNameWrappers) {
            String userAudioFileName = userAudioFileNameWrapper.getUserAudioFileName();
            audioHeadersAndPayloadList.add(getAudioHeader(userAudioFileName) + getAudioPayload(userAudioFileName));
        }
        return audioHeadersAndPayloadList;
    }

    private String getAudioHeader(String fileName) {
        if (fileName.endsWith(".mp3")) {
            return "data:audio/mpeg;base64,";
        }
        if (fileName.endsWith(".wav")) {
            return "data:audio/wav;base64,";
        }
        throw new IllegalArgumentException("unexpected file extension for file " + fileName);
    }

    private String getAudioPayload(String userAudioFileName) {
        if (userAudioBase64Cache.containsKey(userAudioFileName)) {
            return userAudioBase64Cache.get(userAudioFileName);
        }
        if (userAudioBase64Cache.size() > 1000) {
            userAudioBase64Cache.clear();
        }
        File file = userAudioFileCache.get(userAudioFileName);
        byte[] bytes;
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        userAudioBase64Cache.put(userAudioFileName, base64);
        return base64;
    }

    @Override
    public int size() {
        return userAudioFileCache.size();
    }

    @Getter
    private static class UserAudioFileName implements Comparable<UserAudioFileName> {
        private final String userAudioFileName;
        private final String originalFileName;
        private final Long timeMillis;

        UserAudioFileName(String userAudioFileName) {
            this.userAudioFileName = userAudioFileName;
            String[] parts = userAudioFileName.split("-");
            originalFileName = parts[0];
            String[] timeMillisDotExtension = parts[1].split("\\.");
            timeMillis = Long.valueOf(timeMillisDotExtension[0]);
        }

        @Override
        public int compareTo(UserAudioFileName other) {
            return Long.compare(timeMillis, other.getTimeMillis());
        }
    }
}
