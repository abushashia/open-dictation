package com.sitedictation;

import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
class DiffMatchPatchHelper {

    private final CorpusMetadataHelper corpusMetadataHelper;
    private final DiffMatchPatch diffMatchPatch;

    DiffMatchPatchHelper(CorpusMetadataHelper corpusMetadataHelper, DiffMatchPatch diffMatchPatch) {
        this.corpusMetadataHelper = corpusMetadataHelper;
        this.diffMatchPatch = diffMatchPatch;
    }

    String diff(Sentence sentence, String userTranscript) {
        // TODO get more strict with user as streak increases for position being graded
        boolean stripAccents = corpusMetadataHelper.isStripAccents(sentence.getLanguage());
        LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diff_main(
                DictationStringUtils.processTranscript(userTranscript, stripAccents),
                DictationStringUtils.processTranscript(sentence.getTranscript(), stripAccents));
        boolean hasMeaningfulDifference = false;
        for (DiffMatchPatch.Diff diff : diffs) {
            if ((diff.operation == DiffMatchPatch.Operation.DELETE)
                    || (diff.operation == DiffMatchPatch.Operation.INSERT)) {
                if (corpusMetadataHelper.isIgnoreWhitespaceDiffs(sentence.getLanguage())
                        && diff.text.matches("\\s+")) {
                    continue;
                }
                hasMeaningfulDifference = true;
                break;
            }
        }
        if (hasMeaningfulDifference) {
            String prettyHtml = diffMatchPatch.diff_prettyHtml(diffs);
            prettyHtml = prettyHtml.replace("#e6ffe6", "Yellow");
            prettyHtml = prettyHtml.replace("#ffe6e6", "Red");
            // TODO if language setting not strict about whitespace, change color to blue for additional whitespace, like -
            return prettyHtml;
        }
        return null;
    }

    public int computeLevenshteinDistance(Transaction transaction, Sentence sentence) {
        if (transaction.getCorrect()) {
            return 0;
        }
        String userTranscript = transaction.getUserTranscript();
        boolean stripAccents = corpusMetadataHelper.isStripAccents(sentence.getLanguage());
        LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diff_main(
                DictationStringUtils.processTranscript(userTranscript, stripAccents),
                DictationStringUtils.processTranscript(sentence.getTranscript(), stripAccents));
        return diffMatchPatch.diff_levenshtein(diffs);
    }
}
