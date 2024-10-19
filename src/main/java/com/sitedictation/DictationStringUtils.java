package com.sitedictation;

import org.apache.commons.lang3.StringUtils;

class DictationStringUtils {

    static String processTranscript(String transcript, boolean stripAccents) {
        transcript = transcript.trim();
        transcript = transcript.toLowerCase();
        transcript = transcript.replaceAll("\n", " ");
        transcript = transcript.replaceAll("\r", " ");

        // (remove) expressions surrounded (by parentheses) --> expressions surrounded
        transcript = transcript.replaceAll("\\([^)]*\\)", "");

        transcript = transcript.replaceAll("\\p{Cntrl}", " ");
        // transcript = transcript.replaceAll("[^\\p{L}-' ]", "");
        // TODO consider whether to reinstate requirement of hyphens
        transcript = transcript.replaceAll("[^\\p{L} ]", "");
        if (stripAccents) {
            transcript = StringUtils.stripAccents(transcript);
            transcript = transcript.replaceAll("ß", "ss");
        }
        transcript = transcript.replaceAll("\\s{2,}", " ");
        // trim again, in case leading punctuation accompanied by whitespace, like "¿ Qué es? ¿ podéis verlo ?"
        transcript = transcript.trim();
        return transcript;
    }
}
