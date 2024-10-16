package io.quarkus.annotation.processor.documentation.config.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.asciidoctor.Asciidoctor.Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.util.JavadocUtil;

public class JavadocToAsciidocTransformerConfigItemTest {

    @Test
    public void removeParagraphIndentation() {
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc("First paragraph<br><br> Second Paragraph");
        assertEquals("First paragraph +\n +\nSecond Paragraph",
                JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));
    }

    @Test
    public void parseUntrimmedJavaDoc() {
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc("                ");
        assertNull(parsed.description());

        parsed = JavadocUtil.parseConfigItemJavadoc("      <br> </br>          ");
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertNull(description);
    }

    @Test
    public void parseJavaDocWithParagraph() {
        String javaDoc = "hello<p>world</p>";
        String expectedOutput = "hello\n\nworld";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);

        javaDoc = "hello world<p>bonjour </p><p>le monde</p>";
        expectedOutput = "hello world\n\nbonjour\n\nle monde";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithStyles() {
        // Bold
        String javaDoc = "hello <b>world</b>";
        String expectedOutput = "hello *world*";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        javaDoc = "hello <strong>world</strong>";
        expectedOutput = "hello *world*";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // Emphasized
        javaDoc = "<em>hello world</em>";
        expectedOutput = "_hello world_";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // Italics
        javaDoc = "<i>hello world</i>";
        expectedOutput = "_hello world_";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // Underline
        javaDoc = "<u>hello world</u>";
        expectedOutput = "[.underline]#hello world#";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // small
        javaDoc = "<small>quarkus subatomic</small>";
        expectedOutput = "[.small]#quarkus subatomic#";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // big
        javaDoc = "<big>hello world</big>";
        expectedOutput = "[.big]#hello world#";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // line through
        javaDoc = "<del>hello </del><strike>monolith </strike><s>world</s>";
        expectedOutput = "[.line-through]#hello #[.line-through]#monolith #[.line-through]#world#";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);

        // superscript and subscript
        javaDoc = "<sup>cloud </sup><sub>in-premise</sub>";
        expectedOutput = "^cloud ^~in-premise~";
        parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithLiTagsInsideUlTag() {
        String javaDoc = "List:" +
                "<ul>\n" +
                "<li>1</li>\n" +
                "<li>2</li>\n" +
                "</ul>" +
                "";
        String expectedOutput = "List:\n\n - 1\n - 2";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithLiTagsInsideOlTag() {
        String javaDoc = "List:" +
                "<ol>\n" +
                "<li>1</li>\n" +
                "<li>2</li>\n" +
                "</ol>" +
                "";
        String expectedOutput = "List:\n\n . 1\n . 2";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithLinkInlineSnippet() {
        String javaDoc = "{@link firstlink} {@link #secondlink} \n {@linkplain #third.link}";
        String expectedOutput = "`firstlink` `secondlink` `third.link`";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithLinkTag() {
        String javaDoc = "this is a <a href='http://link.com'>hello</a> link";
        String expectedOutput = "this is a link:http://link.com[hello] link";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithCodeInlineSnippet() {
        String javaDoc = "{@code true} {@code false}";
        String expectedOutput = "`true` `false`";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithLiteralInlineSnippet() {
        String javaDoc = "{@literal java.util.Boolean}";
        String expectedOutput = "`java.util.Boolean`";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithValueInlineSnippet() {
        String javaDoc = "{@value 10s}";
        String expectedOutput = "`10s`";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithUnknownInlineSnippet() {
        String javaDoc = "{@see java.util.Boolean}";
        String expectedOutput = "java.util.Boolean";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithUnknownNode() {
        String javaDoc = "<unknown>hello</unknown>";
        String expectedOutput = "hello";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        String description = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());

        assertEquals(expectedOutput, description);
    }

    @Test
    public void parseJavaDocWithBlockquoteBlock() {
        ParsedJavadoc parsed = JavadocUtil
                .parseConfigItemJavadoc("See Section 4.5.5 of the JSR 380 specification, specifically\n"
                        + "\n"
                        + "<blockquote>\n"
                        + "In sub types (be it sub classes/interfaces or interface implementations), no parameter constraints may\n"
                        + "be declared on overridden or implemented methods, nor may parameters be marked for cascaded validation.\n"
                        + "This would pose a strengthening of preconditions to be fulfilled by the caller.\n"
                        + "</blockquote>\nThat was interesting, wasn't it?");

        assertEquals("See Section 4.5.5 of the JSR 380 specification, specifically\n"
                + "\n"
                + "[quote]\n"
                + "____\n"
                + "In sub types (be it sub classes/interfaces or interface implementations), no parameter constraints may be declared on overridden or implemented methods, nor may parameters be marked for cascaded validation. This would pose a strengthening of preconditions to be fulfilled by the caller.\n"
                + "____\n"
                + "\n"
                + "That was interesting, wasn't it?",
                JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));

        parsed = JavadocUtil.parseConfigItemJavadoc(
                "Some HTML entities &amp; special characters:\n\n<pre>&lt;os&gt;|&lt;arch&gt;[/variant]|&lt;os&gt;/&lt;arch&gt;[/variant]\n</pre>\n\nbaz");

        assertEquals(
                "Some HTML entities & special characters:\n\n```\n<os>|<arch>[/variant]|<os>/<arch>[/variant]\n```\n\nbaz",
                JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));

        // TODO
        // assertEquals("Example:\n\n```\nfoo\nbar\n```",
        // JavadocUtil.parseConfigItemJavadoc("Example:\n\n<pre>{@code\nfoo\nbar\n}</pre>"));
    }

    @Test
    public void parseJavaDocWithCodeBlock() {
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc("Example:\n\n<pre>\nfoo\nbar\n</pre>\n\nbaz");

        assertEquals("Example:\n\n```\nfoo\nbar\n```\n\nbaz",
                JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));

        parsed = JavadocUtil.parseConfigItemJavadoc(
                "Some HTML entities &amp; special characters:\n\n<pre>&lt;os&gt;|&lt;arch&gt;[/variant]|&lt;os&gt;/&lt;arch&gt;[/variant]\n</pre>\n\nbaz");

        assertEquals(
                "Some HTML entities & special characters:\n\n```\n<os>|<arch>[/variant]|<os>/<arch>[/variant]\n```\n\nbaz",
                JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));

        // TODO
        // assertEquals("Example:\n\n```\nfoo\nbar\n```",
        // JavadocUtil.parseConfigItemJavadoc("Example:\n\n<pre>{@code\nfoo\nbar\n}</pre>"));
    }

    @Test
    public void asciidoc() {
        String asciidoc = "== My Asciidoc\n" +
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

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(asciidoc + "\n" + "@asciidoclet");

        assertEquals(asciidoc, JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));
    }

    @Test
    public void asciidocLists() {
        String asciidoc = "* A list\n" +
                "\n" +
                "* 1\n" +
                "  * 1.1\n" +
                "  * 1.2\n" +
                "* 2";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(asciidoc + "\n" + "@asciidoclet");

        assertEquals(asciidoc, JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "#", "*", "\\", "[", "]", "|" })
    public void escape(String ch) {
        final String javaDoc = "Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch + " " + ch + ch
                + "</code>, {@code JavaDoc tag " + ch + " " + ch + ch + "}";

        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);
        final String asciiDoc = JavadocToAsciidocTransformer.toAsciidoc(parsed.description(), parsed.format());
        final String actual = Factory.create().convert(asciiDoc, Collections.emptyMap());
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
        final String actual = Factory.create().convert(asciiDoc, Collections.emptyMap());

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
        final String actual = Factory.create().convert(asciiDoc, Collections.emptyMap());
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
        final String actual = Factory.create().convert(asciiDoc, Collections.emptyMap());
        assertEquals(expected, actual);
    }

    @Test
    void trim() {
        assertEquals("+ \nfoo", JavadocToAsciidocTransformer.trim(new StringBuilder("+ \nfoo")));
        assertEquals("+", JavadocToAsciidocTransformer.trim(new StringBuilder(" +")));
        assertEquals("foo", JavadocToAsciidocTransformer.trim(new StringBuilder(" +\nfoo")));
        assertEquals("foo +", JavadocToAsciidocTransformer.trim(new StringBuilder("foo +")));
        assertEquals("foo", JavadocToAsciidocTransformer.trim(new StringBuilder("foo")));
        assertEquals("+", JavadocToAsciidocTransformer.trim(new StringBuilder("+ \n")));
        assertEquals("+", JavadocToAsciidocTransformer.trim(new StringBuilder("   +\n+ \n")));
        assertEquals("", JavadocToAsciidocTransformer.trim(new StringBuilder(" +\n")));
        assertEquals("foo", JavadocToAsciidocTransformer.trim(new StringBuilder(" \n\tfoo")));
        assertEquals("foo", JavadocToAsciidocTransformer.trim(new StringBuilder("foo \n\t")));
        assertEquals("foo", JavadocToAsciidocTransformer.trim(new StringBuilder(" \n\tfoo \n\t")));
        assertEquals("", JavadocToAsciidocTransformer.trim(new StringBuilder("")));
        assertEquals("", JavadocToAsciidocTransformer.trim(new StringBuilder("  \n\t")));
        assertEquals("+", JavadocToAsciidocTransformer.trim(new StringBuilder("   +")));
        assertEquals("", JavadocToAsciidocTransformer.trim(new StringBuilder("   +\n")));
        assertEquals("", JavadocToAsciidocTransformer.trim(new StringBuilder("   +\n +\n")));
        assertEquals("foo +\nbar", JavadocToAsciidocTransformer.trim(new StringBuilder("   foo +\nbar +\n")));
    }

}
