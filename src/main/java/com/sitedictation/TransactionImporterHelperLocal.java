package com.sitedictation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
@Primary
@Profile("local")
@Slf4j
class TransactionImporterHelperLocal implements TransactionImporterHelper {

    private final CommonVoiceProperties commonVoiceProperties;
    private final DictationProperties dictationProperties;

    private final Map<String, File> filesByName = new HashMap<>();

    TransactionImporterHelperLocal(CommonVoiceProperties commonVoiceProperties,
                                   DictationProperties dictationProperties) {
        this.commonVoiceProperties = commonVoiceProperties;
        this.dictationProperties = dictationProperties;
    }

    @Override
    public Collection<String> getFileNames() {
        String importTransactionsLocalDirectory;
        if (commonVoiceProperties.getDirectory() != null) {
            importTransactionsLocalDirectory = commonVoiceProperties.getDirectory() + "transactions/";
        } else {
            importTransactionsLocalDirectory = dictationProperties.getImportTransactionsLocalDirectory();
        }
        File directory = new File(importTransactionsLocalDirectory);
        File[] files = directory.listFiles(f -> isTransactionsFile(f));
        if ((files == null) || (files.length == 0)) {
            return Collections.emptyList();
        }

        // check whether files with timeMillis unexpectedly in main (not temp) transactions directory
        // also record file lengths in bytes, to detect later whether temp file has greater length
        Map<String, Long> prefixToFileLengthInBytes = new HashMap<>();
        for (File file : files) {
            String fileName = file.getName();
            long lengthInBytes = file.length();

            // Fail if file name like transactions-foo@gmail.com-common-voice-validated.fr.tsv.txt-1653125733857.json
            String[] split = fileName.split("\\.json");
            String fileNameWithoutExtension = split[0];
            int lastIndexOfHyphen = fileNameWithoutExtension.lastIndexOf("-");
            String optionalTimeMillis = fileNameWithoutExtension.substring(lastIndexOfHyphen);
            try {
                Long.parseLong(optionalTimeMillis);
                throw new RuntimeException("unexpected temp file among main files: " + fileName);
            } catch (NumberFormatException e) {
                // ignore
            }
            if (prefixToFileLengthInBytes.put(fileNameWithoutExtension, lengthInBytes) != null) {
                throw new RuntimeException("too many transactions files for fileNameWithoutExtension " + fileNameWithoutExtension);
            }
        }

        // add temp files for all main files
        final long timeMillis = System.currentTimeMillis();
        final String tempDirName = importTransactionsLocalDirectory + "temp/";
        for (File file : files) {
            filesByName.put(file.getName(), file);
            // copy latest txns file to temp dir with timestamp, because file with root name will be overwritten
            // in course of execution of program
            String backupFileName = tempDirName + file.getName().replace(".json", "-" + timeMillis + ".json");
            try {
                byte[] bytes = FileUtils.readFileToByteArray(file);
                FileUtils.writeByteArrayToFile(new File(backupFileName), bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // raise a problem if current file for a language/corpus SMALLER THAN backup
        processTempDir(tempDirName, prefixToFileLengthInBytes);
        return filesByName.keySet();
    }

    private static boolean isTransactionsFile(File f) {
        return f.getName().startsWith("transactions") && f.getName().endsWith(".json");
    }

    private void processTempDir(String tempDirName, Map<String, Long> prefixToFileLengthInBytes) {
        File tempDir = new File(tempDirName);
        // Specify json extension in order to avoid including hidden system files, such as .DS_store
        File[] tempFiles = tempDir.listFiles(f -> isTransactionsFile(f));
        if ((tempFiles == null) || (tempFiles.length == 0)) {
            return;
        }
        SortedMap<String, SortedMap<Long, File>> timeMillisFilePairsByPrefix = new TreeMap<>();
        for (File tempFile : tempFiles) {
            // transactions-foo@gmail.com-common-voice-validated.fr.tsv.txt-1653125733857.json
            String tempFileName = tempFile.getName();

            // transactions-foo@gmail.com-common-voice-validated.fr.tsv.txt-1653125733857.json
            // --> transactions-foo@gmail.com-common-voice-validated.fr.tsv.txt
            int lastIndexOfHyphen = tempFileName.lastIndexOf("-");
            String prefix = tempFileName.substring(0, lastIndexOfHyphen);

            Long mainFileLengthInBytes = prefixToFileLengthInBytes.get(prefix);
            if (mainFileLengthInBytes == null) {
                log.warn("no main transactions file for prefix {}", prefix);
            } else {
                if (tempFile.length() > mainFileLengthInBytes) {
                    throw new RuntimeException(String.format("temp file %s has more bytes than main file for prefix %s", tempFileName, prefix));
                }
            }

            // transactions-foo@gmail.com-common-voice-validated.fr.tsv.txt-1653125733857.json
            // --> 1653125733857.json
            // --> 1653125733857
            int lastIndexOfDot = tempFileName.lastIndexOf(".");
            String timeMillisDotJson = tempFileName.substring(lastIndexOfHyphen + 1, lastIndexOfDot);
            Long tempFileTimeMillis = Long.valueOf(timeMillisDotJson);

            SortedMap<Long, File> mapForPrefix = timeMillisFilePairsByPrefix.computeIfAbsent(prefix, k -> new TreeMap<>());
            if (mapForPrefix.put(tempFileTimeMillis, tempFile) != null) {
                throw new RuntimeException(String.format("too many temp files for %s for %d", prefix, tempFileTimeMillis));
            }
        }
        for (SortedMap<Long, File> tempFilesByTimeMillisAsc : timeMillisFilePairsByPrefix.values()) {
            while (tempFilesByTimeMillisAsc.size() > 4) {
                File tempFile = tempFilesByTimeMillisAsc.remove(tempFilesByTimeMillisAsc.firstKey());
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn(String.format("May have failed to delete %s", tempFile.getName()));
                }
            }
        }
    }

    @Override
    public InputStream getInputStream(String fileName) {
        try {
            return new FileInputStream(filesByName.get(fileName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
