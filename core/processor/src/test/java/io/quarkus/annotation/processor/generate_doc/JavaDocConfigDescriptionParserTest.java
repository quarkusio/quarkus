package io.quarkus.annotation.processor.generate_doc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        String expectedOutput = "hello\nworld";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);

        javaDoc = "hello world<p>bonjour </p><p>le monde</p>";
        expectedOutput = "hello world\nbonjour \nle monde";
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

        // Emphasized
        javaDoc = "<em>hello world</em>";
        expectedOutput = "*hello world*";
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
    public void parseJavaDocWithUlTags() {
        String javaDoc = "hello <ul>world</ul>";
        String expectedOutput = "hello world";
        String parsed = parser.parseConfigDescription(javaDoc);

        assertEquals(expectedOutput, parsed);

        javaDoc = "hello world<ul> bonjour </ul><ul>le monde</ul>";
        expectedOutput = "hello world bonjour le monde";
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
        String expectedOutput = "List: \n - 1 \n - 2";
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
        String expectedOutput = "List: \n . 1 \n . 2";
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
}
