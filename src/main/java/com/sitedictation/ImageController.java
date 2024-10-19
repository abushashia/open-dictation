package com.sitedictation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("images")
class ImageController {

    private final ImageService imageService;
    private final SentenceRepository sentenceRepository;

    ImageController(ImageService imageService, SentenceRepository sentenceRepository) {
        this.imageService = imageService;
        this.sentenceRepository = sentenceRepository;
    }

    @GetMapping("{fileName}")
    String getImage(@PathVariable String fileName) {
        Sentence sentence = sentenceRepository.findByFileName(fileName)
                .orElseThrow(() -> new RuntimeException("no sentence for " + fileName));
        String imageLink = imageService.generateImage(sentence.getTranscript());
        return imageLink;
    }
}
