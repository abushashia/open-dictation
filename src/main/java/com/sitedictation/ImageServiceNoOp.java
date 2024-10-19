package com.sitedictation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dictation", value = "image-generation-enabled", havingValue = "false", matchIfMissing = true)
class ImageServiceNoOp implements ImageService {

    @Override
    public String generateImage(String transcript) {
        return null;
    }
}
