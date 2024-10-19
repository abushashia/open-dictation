package com.sitedictation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Primary
@Profile("local")
class TransactionExporterHelperLocal implements TransactionExporterHelper {

    private final CommonVoiceProperties commonVoiceProperties;
    private final DictationProperties dictationProperties;

    TransactionExporterHelperLocal(CommonVoiceProperties commonVoiceProperties,
                                   DictationProperties dictationProperties) {
        this.commonVoiceProperties = commonVoiceProperties;
        this.dictationProperties = dictationProperties;
    }

    @Override
    public void exportTransactions(String transactionsFileName, byte[] bytes) {
        String importTransactionsLocalDirectory;
        if (StringUtils.isNotBlank(commonVoiceProperties.getDirectory())) {
            importTransactionsLocalDirectory = commonVoiceProperties.getDirectory() + "transactions/";
        } else {
            importTransactionsLocalDirectory = dictationProperties.getImportTransactionsLocalDirectory();
        }
        String absolutePath = importTransactionsLocalDirectory + transactionsFileName;
        try {
            FileUtils.writeByteArrayToFile(new File(absolutePath), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
