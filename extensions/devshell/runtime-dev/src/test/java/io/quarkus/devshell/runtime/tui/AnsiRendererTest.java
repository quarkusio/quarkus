package io.quarkus.devshell.runtime.tui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnsiRendererTest {

    // --- stripAnsi ---

    @Test
    void stripAnsiRemovesColorCodes() {
        String colored = "\u001b[31mError\u001b[0m";
        assertThat(AnsiRenderer.stripAnsi(colored)).isEqualTo("Error");
    }

    @Test
    void stripAnsiRemovesBoldAndMultipleCodes() {
        String styled = "\u001b[1m\u001b[36mHeader\u001b[0m";
        assertThat(AnsiRenderer.stripAnsi(styled)).isEqualTo("Header");
    }

    @Test
    void stripAnsiPreservesPlainText() {
        assertThat(AnsiRenderer.stripAnsi("Hello World")).isEqualTo("Hello World");
    }

    @Test
    void stripAnsiHandlesEmptyString() {
        assertThat(AnsiRenderer.stripAnsi("")).isEqualTo("");
    }

    @Test
    void stripAnsiRemovesCursorMovement() {
        String withCursor = "\u001b[5;10HText\u001b[2J";
        assertThat(AnsiRenderer.stripAnsi(withCursor)).isEqualTo("Text");
    }

    // --- fixedWidth ---

    @Test
    void fixedWidthPadsShortText() {
        String result = AnsiRenderer.fixedWidth("Hi", 10);
        assertThat(result).hasSize(10);
        assertThat(result).startsWith("Hi");
    }

    @Test
    void fixedWidthTruncatesLongText() {
        String result = AnsiRenderer.fixedWidth("Hello World, this is a long text", 10);
        assertThat(result).hasSize(10);
        assertThat(result).endsWith("…");
    }

    @Test
    void fixedWidthExactLength() {
        String result = AnsiRenderer.fixedWidth("12345", 5);
        assertThat(result).isEqualTo("12345");
    }

    @Test
    void fixedWidthHandlesNull() {
        String result = AnsiRenderer.fixedWidth(null, 5);
        assertThat(result).hasSize(5);
    }

    // --- wrapText ---

    @Test
    void wrapTextShortLineUnchanged() {
        List<String> lines = AnsiRenderer.wrapText("Short", 80);
        assertThat(lines).containsExactly("Short");
    }

    @Test
    void wrapTextWrapsLongLine() {
        List<String> lines = AnsiRenderer.wrapText("one two three four five", 10);
        assertThat(lines).isNotEmpty();
        for (String line : lines) {
            assertThat(line.length()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    void wrapTextPreservesNewlines() {
        List<String> lines = AnsiRenderer.wrapText("line1\nline2\nline3", 80);
        assertThat(lines).containsExactly("line1", "line2", "line3");
    }

    @Test
    void wrapTextBreaksLongWord() {
        List<String> lines = AnsiRenderer.wrapText("abcdefghijklmnop", 5);
        assertThat(lines).isNotEmpty();
        for (String line : lines) {
            assertThat(line.length()).isLessThanOrEqualTo(5);
        }
        // All characters should be preserved
        String joined = String.join("", lines);
        assertThat(joined).isEqualTo("abcdefghijklmnop");
    }

    @Test
    void wrapTextHandlesEmpty() {
        assertThat(AnsiRenderer.wrapText("", 80)).isEmpty();
        assertThat(AnsiRenderer.wrapText(null, 80)).isEmpty();
    }

    // --- Cursor/Screen methods ---

    @Test
    void moveToProducesCorrectEscapeSequence() {
        assertThat(AnsiRenderer.moveTo(5, 10)).isEqualTo("\u001b[5;10H");
        assertThat(AnsiRenderer.moveTo(1, 1)).isEqualTo("\u001b[1;1H");
    }

    @Test
    void clearScreenProducesCorrectSequence() {
        assertThat(AnsiRenderer.clearScreen()).isEqualTo("\u001b[2J");
    }

    @Test
    void clearLineProducesCorrectSequence() {
        assertThat(AnsiRenderer.clearLine()).isEqualTo("\u001b[2K");
    }

    @Test
    void clearToEndProducesCorrectSequence() {
        assertThat(AnsiRenderer.clearToEnd()).isEqualTo("\u001b[J");
    }

    @Test
    void moveUpProducesCorrectSequence() {
        assertThat(AnsiRenderer.moveUp(3)).isEqualTo("\u001b[3A");
    }

    @Test
    void moveDownProducesCorrectSequence() {
        assertThat(AnsiRenderer.moveDown(2)).isEqualTo("\u001b[2B");
    }

    // --- Constants ---

    @Test
    void constantsAreValidAnsiSequences() {
        // All ANSI constants should start with ESC[
        assertThat(AnsiRenderer.RESET).startsWith("\u001b[");
        assertThat(AnsiRenderer.BOLD).startsWith("\u001b[");
        assertThat(AnsiRenderer.REVERSE).startsWith("\u001b[");
        assertThat(AnsiRenderer.HIDE_CURSOR).startsWith("\u001b[");
        assertThat(AnsiRenderer.SHOW_CURSOR).startsWith("\u001b[");
        assertThat(AnsiRenderer.ENTER_ALTERNATE_SCREEN).startsWith("\u001b[");
        assertThat(AnsiRenderer.EXIT_ALTERNATE_SCREEN).startsWith("\u001b[");
    }

    @Test
    void stripAnsiRemovesOwnConstants() {
        // Verify that constants are properly stripped by stripAnsi
        String text = AnsiRenderer.BOLD + "bold" + AnsiRenderer.RESET;
        assertThat(AnsiRenderer.stripAnsi(text)).isEqualTo("bold");

        text = AnsiRenderer.FG_RED + "red" + AnsiRenderer.RESET;
        assertThat(AnsiRenderer.stripAnsi(text)).isEqualTo("red");

        text = AnsiRenderer.BG_BLUE + AnsiRenderer.FG_WHITE + "styled" + AnsiRenderer.RESET;
        assertThat(AnsiRenderer.stripAnsi(text)).isEqualTo("styled");
    }
}
