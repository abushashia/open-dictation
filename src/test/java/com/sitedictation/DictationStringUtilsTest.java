package com.sitedictation;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class DictationStringUtilsTest {

    @Test
    public void testLongFloatingHyphens() {
        String russianSentence = "Но, Оля, – пробормотала Женя, – что с тобою?";
        String processedTranscript = DictationStringUtils.processTranscript(russianSentence, false);
        Assert.assertEquals("но оля пробормотала женя что с тобою", processedTranscript);
    }

    @Test
    public void testStripAccents() {
        String output = StringUtils.stripAccents("Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ");
        Assert.assertEquals("This is a funky String", output);
    }

    @Test
    public void testStripAccentsSpanish1() {
        String output = StringUtils.stripAccents("ténganse allá");
        Assert.assertEquals("tenganse alla", output);
    }

    @Test
    public void testStripAccentsRomanian() {
        String output = StringUtils.stripAccents("vreau să învăț și să vorbesc mai întâi decât să");
        Assert.assertEquals("vreau sa invat si sa vorbesc mai intai decat sa", output);
    }

    @Test
    public void testSpanishSentenceWithQuestionMarks() {
        String original = "¿ Qué es? ¿ podéis verlo ?";
        String processedTranscriptWithAccents = DictationStringUtils.processTranscript(original, false);
        Assert.assertEquals("qué es podéis verlo", processedTranscriptWithAccents);
        String processedTranscriptWithoutAccents = DictationStringUtils.processTranscript(original, true);
        Assert.assertEquals("que es podeis verlo", processedTranscriptWithoutAccents);
    }

    @Test
    public void testRemoveParenthesesEtc() {
        String original = "vreau să învăț și să vorbesc (mai întâi decât să)";
        String output = DictationStringUtils.processTranscript(original, true);
        Assert.assertEquals("vreau sa invat si sa vorbesc", output);
    }

    @Test
    public void testRemoveParenthesesTwoSet() {
        String original = "(vreau să învăț) și să vorbesc (mai întâi decât să)";
        String output = DictationStringUtils.processTranscript(original, true);
        Assert.assertEquals("si sa vorbesc", output);
    }

    @Test
    public void testTransactionsMainFileName() {
        String mainFileName = "transactions-foo@gmail.com-common-voice-validated.de.tsv.txt.json";
        String[] split = mainFileName.split("\\.json");
        Assert.assertEquals("unexpected number of tokens", 1, split.length);
        Assert.assertEquals("unexpected token", "transactions-foo@gmail.com-common-voice-validated.de.tsv.txt", split[0]);
    }

    @Test
    public void testTransactionsTempFileName() {
        String tempFileName = "transactions-foo@gmail.com-common-voice-validated.de.tsv.txt-1685278292183.json";
        String[] split = tempFileName.split("\\.json");
        Assert.assertEquals("unexpected number of tokens", 1, split.length);

        String token = split[0];
        // assert tempFile also have a timeMillis
        Assert.assertEquals("unexpected token", "transactions-foo@gmail.com-common-voice-validated.de.tsv.txt-1685278292183", split[0]);

        int lastIndexOfHyphen = token.lastIndexOf("-");
        String substring = token.substring(0, lastIndexOfHyphen);
        // this operation should strip the timeMillis, so this substring same as mainFile prefix, gotten by removing .json extension
        Assert.assertEquals("unexpected token", "transactions-foo@gmail.com-common-voice-validated.de.tsv.txt", substring);
    }
}
