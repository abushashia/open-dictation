package com.sitedictation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageClient;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dictation", value = "image-generation-enabled", havingValue = "true")
@Slf4j
class ImageServiceImpl implements ImageService {

    private final ImageClient imageClient;

    ImageServiceImpl(ImageClient imageClient) {
        this.imageClient = imageClient;
    }

    @Override
    public String generateImage(String transcript) {
        ImageOptions imageOptions = ImageOptionsBuilder.builder().build();
        ImagePrompt imagePrompt = new ImagePrompt(transcript, imageOptions);
        ImageResponse response = imageClient.call(imagePrompt);
        String url = response.getResult().getOutput().getUrl();
        log.info("Generated link for \'{}\': {}", transcript, url);
        return url;
    }
}
