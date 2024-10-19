package com.sitedictation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dictation", value = "translation-enabled", havingValue = "false", matchIfMissing = true)
class TranslationServiceNoOp implements TranslationService {

    @Override
    public String translate(String language, String transcript) {
        return null;
    }
}
