package io.quarkus.annotation.processor.documentation.config.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadocSection;

public class JavadocToAsciidocTransformerConfigSectionTest {

    @Test
    public void parseNullSection() {
        ParsedJavadocSection parsed = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(null);
        assertEquals(null, parsed.details());
        assertEquals(null, parsed.title());
    }

    @Test
    public void parseUntrimmedJavaDoc() {
        ParsedJavadocSection parsed = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc("                ");
        assertEquals(null, parsed.title());
        assertEquals(null, parsed.details());

        parsed = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc("      <br> </br>          ");
        assertEquals(null, parsed.title());
        assertEquals(null, parsed.details());
    }

    @Test
    public void passThroughAConfigSectionInAsciiDoc() {
        String title = "My Asciidoc";
        String details = "Let's have a https://quarkus.io[link to our website].\n" +
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

        String asciidoc = "=== " + title + "\n\n" + details;

        ParsedJavadocSection sectionHolder = JavadocToAsciidocTransformer.INSTANCE
                .parseConfigSectionJavadoc(asciidoc + "\n" + "@asciidoclet");
        assertEquals(title, sectionHolder.title());
        assertEquals(details, sectionHolder.details());

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

        sectionHolder = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(asciidoc + "\n" + "@asciidoclet");
        assertEquals("Asciidoc title", sectionHolder.title());
    }

    @Test
    public void parseSectionWithoutIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section";
        String expectedTitle = "Config Section";
        String expectedDetails = null;
        ParsedJavadocSection sectionHolder = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc);
        assertEquals(expectedDetails, sectionHolder.details());
        assertEquals(expectedTitle, sectionHolder.title());

        javaDoc = "Config Section.";
        expectedTitle = "Config Section";
        expectedDetails = null;
        sectionHolder = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc);
        assertEquals(expectedDetails, sectionHolder.details());
        assertEquals(expectedTitle, sectionHolder.title());

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section</p>";
        expectedTitle = "Config Section";
        expectedDetails = null;
        sectionHolder = JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc);
        assertEquals(expectedDetails, sectionHolder.details());
        assertEquals(expectedTitle, sectionHolder.title());
    }

    @Test
    public void parseSectionWithIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section .Introduction";
        String expectedDetails = "Introduction";
        String expectedTitle = "Config Section";
        assertEquals(expectedTitle, JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc).title());
        assertEquals(expectedDetails, JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc).details());

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section </p>. Introduction";
        expectedDetails = "Introduction";
        assertEquals(expectedDetails, JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc).details());
        assertEquals(expectedTitle, JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc).title());
    }

    @Test
    public void properlyParseConfigSectionWrittenInHtml() {
        String javaDoc = "<p>Config Section.</p>This is section introduction";
        String expectedDetails = "This is section introduction";
        String title = "Config Section";
        assertEquals(expectedDetails, JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc).details());
        assertEquals(title, JavadocToAsciidocTransformer.INSTANCE.parseConfigSectionJavadoc(javaDoc).title());
    }
}
