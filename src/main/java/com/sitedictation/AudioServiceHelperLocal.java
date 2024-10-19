package com.sitedictation;

import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Component
@Primary
@Profile("local")
class AudioServiceHelperLocal implements AudioServiceHelper {

    private final CorpusMetadataHelper corpusMetadataHelper;

    AudioServiceHelperLocal(CorpusMetadataHelper corpusMetadataHelper) {
        this.corpusMetadataHelper = corpusMetadataHelper;
    }

    @Override
    public byte[] getAudioPayload(String corpus, String fileName) {
        String dictationDirectory = corpusMetadataHelper.getBucketForCorpus(corpus);
        InputStream resource;
        try {
            resource = new FileInputStream(dictationDirectory + fileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("file not found for " + fileName, e);
        }
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(resource);
        } catch (IOException e) {
            throw new RuntimeException("unexpected exception converting file to byte array, " + fileName);
        }
        return bytes;
    }
}
