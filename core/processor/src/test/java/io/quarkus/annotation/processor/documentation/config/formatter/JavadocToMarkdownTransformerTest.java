package io.quarkus.annotation.processor.documentation.config.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class JavadocToMarkdownTransformerTest {

    // -------------------------------------------------------------------------
    // Null / blank input
    // -------------------------------------------------------------------------

    @Test
    public void nullInputReturnsNull() {
        assertNull(JavadocToMarkdownTransformer.asciidocToMarkdown(null));
    }

    @Test
    public void blankInputReturnsBlank() {
        assertEquals("   ", JavadocToMarkdownTransformer.asciidocToMarkdown("   "));
    }

    // -------------------------------------------------------------------------
    // Plain text (pass-through)
    // -------------------------------------------------------------------------

    @Test
    public void plainTextIsPreserved() {
        String input = "A simple configuration property.";
        assertEquals(input, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    // -------------------------------------------------------------------------
    // Inline bold
    // -------------------------------------------------------------------------

    @Test
    public void inlineBoldIsSingleAsterisk() {
        assertEquals("**important**", JavadocToMarkdownTransformer.asciidocToMarkdown("*important*"));
    }

    @Test
    public void inlineBoldInSentence() {
        assertEquals("Use **this** value.", JavadocToMarkdownTransformer.asciidocToMarkdown("Use *this* value."));
    }

    // -------------------------------------------------------------------------
    // Inline italic
    // -------------------------------------------------------------------------

    @Test
    public void inlineItalicIsUnderscore() {
        assertEquals("*note*", JavadocToMarkdownTransformer.asciidocToMarkdown("_note_"));
    }

    @Test
    public void inlineItalicInSentence() {
        assertEquals("See *also* the docs.", JavadocToMarkdownTransformer.asciidocToMarkdown("See _also_ the docs."));
    }

    // -------------------------------------------------------------------------
    // Inline code (backticks are identical in both formats)
    // -------------------------------------------------------------------------

    @Test
    public void inlineCodeIsUnchanged() {
        assertEquals("`myValue`", JavadocToMarkdownTransformer.asciidocToMarkdown("`myValue`"));
    }

    // -------------------------------------------------------------------------
    // Links
    // -------------------------------------------------------------------------

    @Test
    public void linkWithTextIsConverted() {
        assertEquals("[Quarkus](https://quarkus.io)",
                JavadocToMarkdownTransformer.asciidocToMarkdown("link:https://quarkus.io[Quarkus]"));
    }

    @Test
    public void bareUrlWithTextIsConverted() {
        assertEquals("[the guide](https://quarkus.io/guides/config)",
                JavadocToMarkdownTransformer.asciidocToMarkdown("https://quarkus.io/guides/config[the guide]"));
    }

    @Test
    public void linkWithEmptyTextYieldsUrl() {
        assertEquals("https://quarkus.io",
                JavadocToMarkdownTransformer.asciidocToMarkdown("link:https://quarkus.io[]"));
    }

    @Test
    public void urlColonIsNotTreatedAsDefinitionList() {
        String input = "See https://quarkus.io for more information.";
        assertEquals(input, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    // -------------------------------------------------------------------------
    // Code blocks
    // -------------------------------------------------------------------------

    @Test
    public void codeBlockIsConverted() {
        String input = "Example:\n----\nquarkus.http.port=8080\n----";
        String expected = "Example:\n```\nquarkus.http.port=8080\n```";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    // -------------------------------------------------------------------------
    // Ordered and unordered lists
    // -------------------------------------------------------------------------

    @Test
    public void orderedListItemIsConverted() {
        String input = "Steps:\n. First step\n. Second step";
        String expected = "Steps:\n1. First step\n1. Second step";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    @Test
    public void unorderedListItemIsConverted() {
        String input = "Options:\n* Option A\n* Option B";
        String expected = "Options:\n- Option A\n- Option B";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    // -------------------------------------------------------------------------
    // Definition lists
    // -------------------------------------------------------------------------

    @Test
    public void definitionListTermOnOwnLine() {
        String input = "`simple`::\nThe default strategy.";
        String expected = "**`simple`**: The default strategy.";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    @Test
    public void definitionListInlineDefinition() {
        String input = "`no-alias`:: A strategy without aliases.";
        String expected = "**`no-alias`**: A strategy without aliases.";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    @Test
    public void definitionListMultipleEntries() {
        String input = "`simple`::\nThe default, future-proof strategy.\n\n`no-alias`::\nA strategy without index aliases.";
        String expected = "**`simple`**: The default, future-proof strategy.\n\n**`no-alias`**: A strategy without index aliases.";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }

    @Test
    public void definitionListTermWithoutDefinition() {
        String input = "term::";
        String expected = "**term**:";
        assertEquals(expected, JavadocToMarkdownTransformer.asciidocToMarkdown(input));
    }
}
