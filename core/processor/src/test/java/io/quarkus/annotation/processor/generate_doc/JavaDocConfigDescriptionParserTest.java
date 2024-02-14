package io.quarkus.annotation.processor.generate_doc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.asciidoctor.Asciidoctor.Factory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JavaDocConfigDescriptionParserTest {

    private JavaDocParser parser;

    @BeforeEach
    public void setup() {
        parser = new JavaDocParser();
    }

    @Test
    public void parseNullJavaDoc() {
        String parsed = parser.parseConfigDescription(null);
        assertEquals("", parsed);
    }

    @Test
    public void removeParagraphIndentation() {
        String parsed = parser.parseConfigDescription("First paragraph<br><br> Second Paragraph");
        assertEquals("First paragraph +\n +\nSecond Paragraph", parsed);
    }

    @Test
    public void parseUntrimmedJavaDoc() {
        String parsed = parser.parseConfigDescription("                ");
        assertEquals("", parsed);
        parsed = parser.parseConfigDescription("      <br> </br>          ");
        assertEquals("", parsed);
    }

    @Test
    public void parseSimpleJavaDoc() {
        String javaDoc = "hello world";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(javaDoc, parsed);
    }

    @Test
    public void parseJavaDocWithParagraph() {
        String javaDoc = "hello<p>world</p>";
        String expectedOutput = "hello\n\nworld";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);

        javaDoc = "hello world<p>bonjour </p><p>le monde</p>";
        expectedOutput = "hello world\n\nbonjour\n\nle monde";
        parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithStyles() {
        // Bold
        String javaDoc = "hello <b>world</b>";
        String expectedOutput = "hello *world*";
        String parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        javaDoc = "hello <strong>world</strong>";
        expectedOutput = "hello *world*";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // Emphasized
        javaDoc = "<em>hello world</em>";
        expectedOutput = "_hello world_";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // Italics
        javaDoc = "<i>hello world</i>";
        expectedOutput = "_hello world_";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // Underline
        javaDoc = "<u>hello world</u>";
        expectedOutput = "[.underline]#hello world#";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // small
        javaDoc = "<small>quarkus subatomic</small>";
        expectedOutput = "[.small]#quarkus subatomic#";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // big
        javaDoc = "<big>hello world</big>";
        expectedOutput = "[.big]#hello world#";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // line through
        javaDoc = "<del>hello </del><strike>monolith </strike><s>world</s>";
        expectedOutput = "[.line-through]#hello #[.line-through]#monolith #[.line-through]#world#";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);

        // superscript and subscript
        javaDoc = "<sup>cloud </sup><sub>in-premise</sub>";
        expectedOutput = "^cloud ^~in-premise~";
        parsed = parser.parseConfigDescription(javaDoc);
        assertEquals(expectedOutput, parsed);
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
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
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
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithLinkInlineSnippet() {
        String javaDoc = "{@link firstlink} {@link #secondlink} \n {@linkplain #third.link}";
        String expectedOutput = "`firstlink` `secondlink` `third.link`";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithLinkTag() {
        String javaDoc = "this is a <a href='http://link.com'>hello</a> link";
        String expectedOutput = "this is a link:http://link.com[hello] link";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithCodeInlineSnippet() {
        String javaDoc = "{@code true} {@code false}";
        String expectedOutput = "`true` `false`";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithLiteralInlineSnippet() {
        String javaDoc = "{@literal java.util.Boolean}";
        String expectedOutput = "`java.util.Boolean`";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithValueInlineSnippet() {
        String javaDoc = "{@value 10s}";
        String expectedOutput = "`10s`";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithUnknownInlineSnippet() {
        String javaDoc = "{@see java.util.Boolean}";
        String expectedOutput = "java.util.Boolean";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithUnknownNode() {
        String javaDoc = "<unknown>hello</unknown>";
        String expectedOutput = "hello";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);
    }

    @Test
    public void parseJavaDocWithBlockquoteBlock() {
        assertEquals("See Section 4.5.5 of the JSR 380 specification, specifically\n"
                + "\n"
                + "[quote]\n"
                + "____\n"
                + "In sub types (be it sub classes/interfaces or interface implementations), no parameter constraints may be declared on overridden or implemented methods, nor may parameters be marked for cascaded validation. This would pose a strengthening of preconditions to be fulfilled by the caller.\n"
                + "____\n"
                + "\n"
                + "That was interesting, wasn't it?",
                parser.parseConfigDescription("See Section 4.5.5 of the JSR 380 specification, specifically\n"
                        + "\n"
                        + "<blockquote>\n"
                        + "In sub types (be it sub classes/interfaces or interface implementations), no parameter constraints may\n"
                        + "be declared on overridden or implemented methods, nor may parameters be marked for cascaded validation.\n"
                        + "This would pose a strengthening of preconditions to be fulfilled by the caller.\n"
                        + "</blockquote>\nThat was interesting, wasn't it?"));

        assertEquals(
                "Some HTML entities & special characters:\n\n```\n<os>|<arch>[/variant]|<os>/<arch>[/variant]\n```\n\nbaz",
                parser.parseConfigDescription(
                        "Some HTML entities &amp; special characters:\n\n<pre>&lt;os&gt;|&lt;arch&gt;[/variant]|&lt;os&gt;/&lt;arch&gt;[/variant]\n</pre>\n\nbaz"));

        // TODO
        // assertEquals("Example:\n\n```\nfoo\nbar\n```",
        // parser.parseConfigDescription("Example:\n\n<pre>{@code\nfoo\nbar\n}</pre>"));
    }

    @Test
    public void parseJavaDocWithCodeBlock() {
        assertEquals("Example:\n\n```\nfoo\nbar\n```\n\nbaz",
                parser.parseConfigDescription("Example:\n\n<pre>\nfoo\nbar\n</pre>\n\nbaz"));

        assertEquals(
                "Some HTML entities & special characters:\n\n```\n<os>|<arch>[/variant]|<os>/<arch>[/variant]\n```\n\nbaz",
                parser.parseConfigDescription(
                        "Some HTML entities &amp; special characters:\n\n<pre>&lt;os&gt;|&lt;arch&gt;[/variant]|&lt;os&gt;/&lt;arch&gt;[/variant]\n</pre>\n\nbaz"));

        // TODO
        // assertEquals("Example:\n\n```\nfoo\nbar\n```",
        // parser.parseConfigDescription("Example:\n\n<pre>{@code\nfoo\nbar\n}</pre>"));
    }

    @Test
    public void since() {
        AtomicReference<String> javadoc = new AtomicReference<>();
        AtomicReference<String> since = new AtomicReference<>();
        parser.parseConfigDescription("Javadoc text\n\n@since 1.2.3", javadoc::set, since::set);
        assertEquals("Javadoc text", javadoc.get());
        assertEquals("1.2.3", since.get());
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

        assertEquals(asciidoc, parser.parseConfigDescription(asciidoc + "\n" + "@asciidoclet"));
    }

    @Test
    public void asciidocLists() {
        String asciidoc = "* A list\n" +
                "\n" +
                "* 1\n" +
                "  * 1.1\n" +
                "  * 1.2\n" +
                "* 2";

        assertEquals(asciidoc, parser.parseConfigDescription(asciidoc + "\n" + "@asciidoclet"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "#", "*", "\\", "[", "]", "|" })
    public void escape(String ch) {
        final String javaDoc = "Inline " + ch + " " + ch + ch + ", <code>HTML tag glob " + ch + " " + ch + ch
                + "</code>, {@code JavaDoc tag " + ch + " " + ch + ch + "}";

        final String asciiDoc = parser.parseConfigDescription(javaDoc);
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

        final String asciiDoc = new JavaDocParser(true).parseConfigDescription(javaDoc);
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

        final String asciiDoc = parser.parseConfigDescription(javaDoc);
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

        final String asciiDoc = parser.parseConfigDescription(javaDoc);
        final String actual = Factory.create().convert(asciiDoc, Collections.emptyMap());
        assertEquals(expected, actual);
    }

    @Test
    void trim() {
        assertEquals("+ \nfoo", JavaDocParser.trim(new StringBuilder("+ \nfoo")));
        assertEquals("+", JavaDocParser.trim(new StringBuilder(" +")));
        assertEquals("foo", JavaDocParser.trim(new StringBuilder(" +\nfoo")));
        assertEquals("foo +", JavaDocParser.trim(new StringBuilder("foo +")));
        assertEquals("foo", JavaDocParser.trim(new StringBuilder("foo")));
        assertEquals("+", JavaDocParser.trim(new StringBuilder("+ \n")));
        assertEquals("+", JavaDocParser.trim(new StringBuilder("   +\n+ \n")));
        assertEquals("", JavaDocParser.trim(new StringBuilder(" +\n")));
        assertEquals("foo", JavaDocParser.trim(new StringBuilder(" \n\tfoo")));
        assertEquals("foo", JavaDocParser.trim(new StringBuilder("foo \n\t")));
        assertEquals("foo", JavaDocParser.trim(new StringBuilder(" \n\tfoo \n\t")));
        assertEquals("", JavaDocParser.trim(new StringBuilder("")));
        assertEquals("", JavaDocParser.trim(new StringBuilder("  \n\t")));
        assertEquals("+", JavaDocParser.trim(new StringBuilder("   +")));
        assertEquals("", JavaDocParser.trim(new StringBuilder("   +\n")));
        assertEquals("", JavaDocParser.trim(new StringBuilder("   +\n +\n")));
        assertEquals("foo +\nbar", JavaDocParser.trim(new StringBuilder("   foo +\nbar +\n")));
    }

}
