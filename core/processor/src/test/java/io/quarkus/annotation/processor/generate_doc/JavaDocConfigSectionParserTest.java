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
        JavaDocParser.SectionHolder parsed = parser.parseConfigSection(null, 1);
        assertEquals("", parsed.details);
        assertEquals("", parsed.title);
    }

    @Test
    public void parseUntrimmedJavaDoc() {
        JavaDocParser.SectionHolder parsed = parser.parseConfigSection("                ", 1);
        assertEquals("", parsed.details);
        assertEquals("", parsed.title);

        parsed = parser.parseConfigSection("      <br> </br>          ", 1);
        assertEquals("", parsed.details);
        assertEquals("", parsed.title);
    }

    @Test
    public void passThroughAConfigSectionInAsciiDoc() {
        String asciidoc = "=== My Asciidoc\n" +
                "\n" +
                ".Let's have a https://quarkus.io[link to our website].\n" +
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

        JavaDocParser.SectionHolder sectionHolder = parser.parseConfigSection(asciidoc + "\n" + "@asciidoclet", 1);
        assertEquals(asciidoc, sectionHolder.details);
        assertEquals("My Asciidoc", sectionHolder.title);

        asciidoc = "Asciidoc title. \n" +
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

        sectionHolder = parser.parseConfigSection(asciidoc + "\n" + "@asciidoclet", 1);
        assertEquals("Asciidoc title", sectionHolder.title);
    }

    @Test
    public void parseSectionWithoutIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section";
        String expectedTitle = "Config Section";
        String expectedDetails = "== Config Section";
        JavaDocParser.SectionHolder sectionHolder = parser.parseConfigSection(javaDoc, 1);
        assertEquals(expectedDetails, sectionHolder.details);
        assertEquals(expectedTitle, sectionHolder.title);

        javaDoc = "Config Section.";
        expectedTitle = "Config Section";
        expectedDetails = "== Config Section";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 1).details);
        assertEquals(expectedTitle, sectionHolder.title);

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section</p>";
        expectedTitle = "Config Section";
        expectedDetails = "== Config Section";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 1).details);
        assertEquals(expectedTitle, sectionHolder.title);
    }

    @Test
    public void parseSectionWithIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section .Introduction";
        String expectedDetails = "== Config Section\n\nIntroduction";
        String expectedTitle = "Config Section";
        assertEquals(expectedTitle, parser.parseConfigSection(javaDoc, 1).title);
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 1).details);

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section </p>. Introduction";
        expectedDetails = "== Config Section\n\nIntroduction";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 1).details);
        assertEquals(expectedTitle, parser.parseConfigSection(javaDoc, 1).title);
    }

    @Test
    public void properlyParseConfigSectionWrittenInHtml() {
        String javaDoc = "<p>Config Section.</p>This is section introduction";
        String expectedDetails = "== Config Section\n\nThis is section introduction";
        String title = "Config Section";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 1).details);
        assertEquals(title, parser.parseConfigSection(javaDoc, 1).title);
    }

    @Test
    public void handleSectionLevelCorrectly() {
        String javaDoc = "<p>Config Section.</p>This is section introduction";

        // level 0  should default to 1
        String expectedDetails = "= Config Section\n\nThis is section introduction";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 0).details);

        // level 1
        expectedDetails = "== Config Section\n\nThis is section introduction";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 1).details);

        // level 2
        expectedDetails = "=== Config Section\n\nThis is section introduction";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 2).details);

        // level 3
        expectedDetails = "==== Config Section\n\nThis is section introduction";
        assertEquals(expectedDetails, parser.parseConfigSection(javaDoc, 3).details);
    }
}
