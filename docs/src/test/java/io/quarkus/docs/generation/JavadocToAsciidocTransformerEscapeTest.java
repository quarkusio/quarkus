package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.asciidoctor.Asciidoctor.Factory;
import org.asciidoctor.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.formatter.JavadocToAsciidocTransformer;
import io.quarkus.annotation.processor.documentation.config.util.JavadocUtil;

/**
 * Converts the AsciiDoc that {@link JavadocToAsciidocTransformer} produces with Asciidoctor, to check the escaping.
 * These tests live here because this module sets the AsciidoctorJ version.
 */
public class JavadocToAsciidocTransformerEscapeTest {

    @ParameterizedTest
    @ValueSource(strings = { "#", "*", "\\", "[", "]", "|" })
    public void escape(String ch) {
        final String javaDoc = "Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch + " " + ch + ch
                + "</code>, {@code JavaDoc tag " + ch + " " + ch + ch + "}";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        final String expected = "<div class=\"paragraph\">\n<p>Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch
                + " " + ch + ch + "</code>, <code>JavaDoc tag " + ch + " " + ch + ch + "</code></p>\n</div>";
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "#", "*", "\\", "[", "]", "|" })
    public void escapeInsideInlineElement(String ch) {
        final String javaDoc = "Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch + " " + ch + ch
                + "</code>, {@code JavaDoc tag " + ch + " " + ch + ch + "}";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format(), true);
        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());

        if (ch.equals("]")) {
            ch = "&#93;";
        }
        final String expected = "<div class=\"paragraph\">\n<p>Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch
                + " " + ch + ch + "</code>, <code>JavaDoc tag " + ch + " " + ch + ch + "</code></p>\n</div>";
        assertEquals(expected, actual);
    }

    @Test
    public void escapePlus() {
        final String javaDoc = "Inline + ++, <code>HTML tag glob + ++</code>, {@code JavaDoc tag + ++}";
        final String expected = "<div class=\"paragraph\">\n<p>Inline &#43; &#43;&#43;, <code>HTML tag glob &#43; &#43;&#43;</code>, <code>JavaDoc tag &#43; &#43;&#43;</code></p>\n</div>";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{", "}" })
    public void escapeBrackets(String ch) {
        final String javaDoc = "Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch + " " + ch + ch
                + "</code>";
        final String expected = "<div class=\"paragraph\">\n<p>Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch
                + " " + ch + ch + "</code></p>\n</div>";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        assertEquals(expected, actual);
    }

    @Test
    public void escapeDoubleAngleBracketsInCodeTag() {
        final String javaDoc = "Use {@code <<empty>>} here.";
        final String expectedAsciidoc = "Use `++<<++empty>>` here.";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedAsciidoc, asciiDoc);

        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        final String expected = "<div class=\"paragraph\">\n<p>Use <code>&lt;&lt;empty&gt;&gt;</code> here.</p>\n</div>";
        assertEquals(expected, actual);
    }

    @Test
    public void escapeDoubleAngleBracketsInHtmlCodeTag() {
        final String javaDoc = "Use <code>&lt;&lt;empty&gt;&gt;</code> here.";
        final String expectedAsciidoc = "Use `++<<++empty>>` here.";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedAsciidoc, asciiDoc);

        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        final String expected = "<div class=\"paragraph\">\n<p>Use <code>&lt;&lt;empty&gt;&gt;</code> here.</p>\n</div>";
        assertEquals(expected, actual);
    }

    @Test
    public void escapeDoubleAngleBracketsInTableCell() {
        final String javaDoc = "<table><tr><th>Name</th></tr><tr><td>{@code <<empty>>}</td></tr></table>";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        assertEquals(false, actual.contains("xref"));
        assertEquals(true, actual.contains("&lt;&lt;empty&gt;&gt;"));
    }

    @Test
    public void doNotEscapeSingleAngleBracket() {
        final String javaDoc = "A <code>List&lt;String&gt;</code> and a {@code List<String>}.";
        final String expectedAsciidoc = "A `List<String>` and a `List<String>`.";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedAsciidoc, asciiDoc);

        final String actual = Factory.create().convert(asciiDoc, Options.builder().build());
        final String expected = "<div class=\"paragraph\">\n<p>A <code>List&lt;String&gt;</code> and a <code>List&lt;String&gt;</code>.</p>\n</div>";
        assertEquals(expected, actual);
    }
}
