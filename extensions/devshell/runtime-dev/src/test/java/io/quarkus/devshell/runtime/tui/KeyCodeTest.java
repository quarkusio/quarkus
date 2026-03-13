package io.quarkus.devshell.runtime.tui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeyCodeTest {

    // --- parse: single character ---

    @Test
    void parseSingleCharReturnsChar() {
        assertThat(KeyCode.parse(new int[] { 'a' })).isEqualTo('a');
        assertThat(KeyCode.parse(new int[] { 'Z' })).isEqualTo('Z');
        assertThat(KeyCode.parse(new int[] { '5' })).isEqualTo('5');
    }

    @Test
    void parseEnterKey() {
        assertThat(KeyCode.parse(new int[] { '\r' })).isEqualTo(KeyCode.ENTER);
    }

    @Test
    void parseEscapeKey() {
        assertThat(KeyCode.parse(new int[] { 27 })).isEqualTo(KeyCode.ESCAPE);
    }

    @Test
    void parseBackspaceKey() {
        assertThat(KeyCode.parse(new int[] { 127 })).isEqualTo(KeyCode.BACKSPACE);
    }

    @Test
    void parseTabKey() {
        assertThat(KeyCode.parse(new int[] { '\t' })).isEqualTo(KeyCode.TAB);
    }

    // --- parse: arrow keys (ESC [ X) ---

    @Test
    void parseArrowUp() {
        assertThat(KeyCode.parse(new int[] { 27, '[', 'A' })).isEqualTo(KeyCode.UP);
    }

    @Test
    void parseArrowDown() {
        assertThat(KeyCode.parse(new int[] { 27, '[', 'B' })).isEqualTo(KeyCode.DOWN);
    }

    @Test
    void parseArrowRight() {
        assertThat(KeyCode.parse(new int[] { 27, '[', 'C' })).isEqualTo(KeyCode.RIGHT);
    }

    @Test
    void parseArrowLeft() {
        assertThat(KeyCode.parse(new int[] { 27, '[', 'D' })).isEqualTo(KeyCode.LEFT);
    }

    // --- parse: special keys (ESC [ N ~) ---

    @Test
    void parseHomeKey() {
        assertThat(KeyCode.parse(new int[] { 27, '[', 'H' })).isEqualTo(KeyCode.HOME);
    }

    @Test
    void parseHomeKeyAlternate() {
        assertThat(KeyCode.parse(new int[] { 27, '[', '1', '~' })).isEqualTo(KeyCode.HOME);
    }

    @Test
    void parseEndKey() {
        assertThat(KeyCode.parse(new int[] { 27, '[', 'F' })).isEqualTo(KeyCode.END);
    }

    @Test
    void parseEndKeyAlternate() {
        assertThat(KeyCode.parse(new int[] { 27, '[', '4', '~' })).isEqualTo(KeyCode.END);
    }

    @Test
    void parsePageUp() {
        assertThat(KeyCode.parse(new int[] { 27, '[', '5', '~' })).isEqualTo(KeyCode.PAGE_UP);
    }

    @Test
    void parsePageDown() {
        assertThat(KeyCode.parse(new int[] { 27, '[', '6', '~' })).isEqualTo(KeyCode.PAGE_DOWN);
    }

    @Test
    void parseDeleteKey() {
        assertThat(KeyCode.parse(new int[] { 27, '[', '3', '~' })).isEqualTo(KeyCode.DELETE);
    }

    // --- parse: edge cases ---

    @Test
    void parseNullReturnsInvalid() {
        assertThat(KeyCode.parse(null)).isEqualTo(-100);
    }

    @Test
    void parseEmptyReturnsInvalid() {
        assertThat(KeyCode.parse(new int[0])).isEqualTo(-100);
    }

    @Test
    void parseUnrecognizedEscapeSequenceFallsBack() {
        // ESC [ Z is not a recognized sequence — should fall back to ESC
        assertThat(KeyCode.parse(new int[] { 27, '[', 'Z' })).isEqualTo(27);
    }

    @Test
    void parseIncompletePageUpFallsBack() {
        // ESC [ 5 without ~ should fall back
        assertThat(KeyCode.parse(new int[] { 27, '[', '5' })).isEqualTo(27);
    }

    // --- isPrintable ---

    @Test
    void isPrintableForRegularChars() {
        assertThat(KeyCode.isPrintable('a')).isTrue();
        assertThat(KeyCode.isPrintable('Z')).isTrue();
        assertThat(KeyCode.isPrintable(' ')).isTrue();
        assertThat(KeyCode.isPrintable('~')).isTrue();
    }

    @Test
    void isPrintableFalseForControlChars() {
        assertThat(KeyCode.isPrintable('\t')).isFalse();
        assertThat(KeyCode.isPrintable('\r')).isFalse();
        assertThat(KeyCode.isPrintable(27)).isFalse();
        assertThat(KeyCode.isPrintable(127)).isFalse();
    }

    @Test
    void isPrintableFalseForNegative() {
        assertThat(KeyCode.isPrintable(KeyCode.UP)).isFalse();
        assertThat(KeyCode.isPrintable(KeyCode.DOWN)).isFalse();
    }

    // --- isLetter ---

    @Test
    void isLetterForLetters() {
        assertThat(KeyCode.isLetter('a')).isTrue();
        assertThat(KeyCode.isLetter('z')).isTrue();
        assertThat(KeyCode.isLetter('A')).isTrue();
        assertThat(KeyCode.isLetter('Z')).isTrue();
    }

    @Test
    void isLetterFalseForDigitsAndSymbols() {
        assertThat(KeyCode.isLetter('0')).isFalse();
        assertThat(KeyCode.isLetter('!')).isFalse();
        assertThat(KeyCode.isLetter(' ')).isFalse();
    }

    // --- isDigit ---

    @Test
    void isDigitForDigits() {
        assertThat(KeyCode.isDigit('0')).isTrue();
        assertThat(KeyCode.isDigit('9')).isTrue();
    }

    @Test
    void isDigitFalseForNonDigits() {
        assertThat(KeyCode.isDigit('a')).isFalse();
        assertThat(KeyCode.isDigit('!')).isFalse();
    }

    // --- toUpperCase ---

    @Test
    void toUpperCaseConvertsLowercase() {
        assertThat(KeyCode.toUpperCase('a')).isEqualTo('A');
        assertThat(KeyCode.toUpperCase('z')).isEqualTo('Z');
    }

    @Test
    void toUpperCaseLeavesUppercaseAlone() {
        assertThat(KeyCode.toUpperCase('A')).isEqualTo('A');
        assertThat(KeyCode.toUpperCase('Z')).isEqualTo('Z');
    }

    @Test
    void toUpperCaseLeavesNonLettersAlone() {
        assertThat(KeyCode.toUpperCase('5')).isEqualTo('5');
        assertThat(KeyCode.toUpperCase('!')).isEqualTo('!');
    }
}
