package io.quarkus.annotation.processor.documentation.config.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadocSection;
import io.quarkus.annotation.processor.documentation.config.util.JavadocUtil;

public class JavadocToAsciidocTransformerConfigSectionTest {

    @Test
    public void parseUntrimmedJavaDoc() {
        ParsedJavadocSection parsed = JavadocUtil.parseConfigSectionJavadoc("                ");
        assertEquals(null, JavadocToAsciidocTransformer.toAsciidoc(parsed.title(), parsed.format()));
        assertEquals(null, JavadocToAsciidocTransformer.toAsciidoc(parsed.details(), parsed.format()));

        parsed = JavadocUtil.parseConfigSectionJavadoc("      <br> </br>          ");
        assertEquals(null, JavadocToAsciidocTransformer.toAsciidoc(parsed.title(), parsed.format()));
        assertEquals(null, JavadocToAsciidocTransformer.toAsciidoc(parsed.details(), parsed.format()));
    }

    @Test
    public void passThroughAConfigSectionInAsciiDoc() {
        String title = "My Asciidoc";
        String details = """
                Let's have a https://quarkus.io[link to our website].

                [TIP]
                ====
                A nice tip
                ====

                [source,java]
                ----
                And some code
                ----""";

        String asciidoc = "=== " + title + "\n\n" + details;

        ParsedJavadocSection sectionHolder = JavadocUtil.parseConfigSectionJavadoc(asciidoc + "\n" + "@asciidoclet");
        assertEquals(title, JavadocToAsciidocTransformer.toAsciidoc(sectionHolder.title(), sectionHolder.format()));
        assertEquals(details, JavadocToAsciidocTransformer.toAsciidoc(sectionHolder.details(), sectionHolder.format()));

        asciidoc = """
                Asciidoc title.\s

                Let's have a https://quarkus.io[link to our website].

                [TIP]
                ====
                A nice tip
                ====

                [source,java]
                ----
                And some code
                ----""";

        sectionHolder = JavadocUtil.parseConfigSectionJavadoc(asciidoc + "\n" + "@asciidoclet");
        assertEquals("Asciidoc title", JavadocToAsciidocTransformer.toAsciidoc(sectionHolder.title(), sectionHolder.format()));
    }
}
