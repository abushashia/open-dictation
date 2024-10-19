package com.sitedictation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile("local")
class SentenceImporterHelperLocal implements SentenceImporterHelper {

    private final CommonVoiceProperties commonVoiceProperties;
    private final DictationProperties dictationProperties;
    private final Map<String, File> filesByName = new HashMap<>();

    SentenceImporterHelperLocal(CommonVoiceProperties commonVoiceProperties,
                                DictationProperties dictationProperties) {
        this.commonVoiceProperties = commonVoiceProperties;
        this.dictationProperties = dictationProperties;
    }

    @Override
    public Collection<String> getCorpora() {
        File directory;
        File[] files;
        if (StringUtils.isNotBlank(commonVoiceProperties.getDirectory())) {
            directory = new File(commonVoiceProperties.getDirectory());
            files = directory.listFiles(
                    file -> file.getName().equals("validated.tsv"));
        } else {
            directory = new File(dictationProperties.getImportSentencesLocalDirectory());
            files = directory.listFiles(
                    file -> file.getName().endsWith(".tsv.txt"));
        }
        if ((files == null) || (files.length == 0)) {
            throw new RuntimeException("no files in sentence directory");
        }
        for (File file : files) {
            filesByName.put(file.getName(), file);
        }
        return filesByName.keySet();
    }

    @Override
    public InputStream getInputStream(String corpus) {
        try {
            return new FileInputStream(filesByName.get(corpus));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
