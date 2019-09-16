package io.quarkus.annotation.processor.generate_doc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JavaDocConfigSectionParserTest {

    private JavaDocParser parser;

    @BeforeEach
    public void setup() {
        parser = new JavaDocParser();
    }

    @Test
    public void parseNullSection() {
        String parsed = parser.parseConfigSection(null, 1);
        assertEquals("", parsed);
    }

    @Test
    public void parseUntrimmedJavaDoc() {
        String parsed = parser.parseConfigSection("                ", 1);
        assertEquals("", parsed);

        parsed = parser.parseConfigSection("      <br> </br>          ", 1);
        assertEquals("", parsed);
    }

    @Test
    public void passThroughAConfigSectionInAsciiDoc() {
        String asciidoc = "=== My Asciidoc\n" +
                "\n" +
                "Let's have a https://quarkus.io[link to our website].\n" +
                "\n" +
                "[TIP]\n" +
                "====\n" +
                "A nice tip\n" +
                "====\n" +
                "\n" +
                "[source,java]\n" +
                "----\n" +
                "And some code\n" +
                "----";

        assertEquals(asciidoc, parser.parseConfigSection(asciidoc + "\n" + "@asciidoclet", 1));
    }

    @Test
    public void parseSectionWithoutIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section";
        String expected = "== Config Section";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));

        javaDoc = "Config Section.";
        expected = "== Config Section";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section</p>";
        expected = "== Config Section";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));
    }

    @Test
    public void parseSectionWithIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section .Introduction";
        String expected = "== Config Section\n\nIntroduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section </p>. Introduction";
        expected = "== Config Section\n\nIntroduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));
    }

    @Test
    public void properlyParseConfigSectionWrittenInHtml() {
        String javaDoc = "<p>Config Section.</p>This is section introduction";
        String expected = "== Config Section\n\nThis is section introduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));
    }

    @Test
    public void handleSectionLevelCorrectly() {
        String javaDoc = "<p>Config Section.</p>This is section introduction";

        // level 0  should default to 1
        String expected = "= Config Section\n\nThis is section introduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 0));

        // level 1
        expected = "== Config Section\n\nThis is section introduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 1));

        // level 2
        expected = "=== Config Section\n\nThis is section introduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 2));

        // level 3
        expected = "==== Config Section\n\nThis is section introduction";
        assertEquals(expected, parser.parseConfigSection(javaDoc, 3));
    }
}
