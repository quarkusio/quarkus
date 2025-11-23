package io.quarkus.annotation.processor.documentation.config.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public class AsciidocToMarkdownTransformerTest {

    @Test
    public void testSourceBlocks() {
        String asciidoc = """
                [source,java]
                ----
                System.out.println("Hello, World!");
                ----""";
        String expectedMarkdown = """
                ```java
                System.out.println("Hello, World!");
                ```""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testQuoteBlocks() {
        String asciidoc = """
                [quote, John Doe]
                ____
                This is a quote.
                ____""";
        String expectedMarkdown = """
                > This is a quote.
                >
                > — John Doe""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testAdmonitions() {
        String asciidoc = "NOTE: This is a note.";
        String expectedMarkdown = "> 📌 This is a note.";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testAdmonitionBlock() {
        String asciidoc = """
                [NOTE]
                ====
                This is an admonition block.
                ====""";
        String expectedMarkdown = """
                > [!NOTE]
                > This is an admonition block.""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testQuotes() {
        String asciidoc = "_This is italic_, *This is bold*";
        String expectedMarkdown = "_This is italic_, **This is bold**";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testLists() {
        String asciidoc = """
                * Item 1
                * Item 2
                * Item 3""";
        String expectedMarkdown = """
                - Item 1
                - Item 2
                - Item 3""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testLinks() {
        String asciidoc = "https://example.com[Example]";
        String expectedMarkdown = "[Example](https://example.com)";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));

        asciidoc = "link:https://example.com[Example]";
        expectedMarkdown = "[Example](https://example.com)";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));

        // TODO render the attributes
        asciidoc = "link:{hibernate-search-docs-url}#backend-elasticsearch-configuration-index-settings[this section of the reference documentation]";
        expectedMarkdown = "[this section of the reference documentation]({hibernate-search-docs-url}#backend-elasticsearch-configuration-index-settings)";
        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testXrefs() {
        String asciidoc = "xref:example.adoc[Example]";
        String expectedMarkdown = "[Example](https://quarkus.io/guides/example)";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));

        asciidoc = "xref:example.adoc#test[Example]";
        expectedMarkdown = "[Example](https://quarkus.io/guides/example#test)";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));

        asciidoc = "xref:test[Example]";
        expectedMarkdown = "[Example](#test)";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testComposite() {
        String asciidoc = """
                Here is some regular text.

                *This is bold text* and visit https://example.com[Example] for more info.

                [source,java]
                ----
                System.out.println("Hello, World!");
                ----

                A quote block:
                [quote, John Doe]
                ____
                This is a quote.
                ____

                A list:
                * Item 1
                * Item 2
                * Item 3""";
        String expectedMarkdown = """
                Here is some regular text.

                **This is bold text** and visit [Example](https://example.com) for more info.

                ```java
                System.out.println("Hello, World!");
                ```

                A quote block:
                > This is a quote.
                >
                > — John Doe

                A list:
                - Item 1
                - Item 2
                - Item 3""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testIcons() {
        String asciidoc = "This looks right with role: icon:check[role=lime] and without role: icon:check[] and this looks bad with role: icon:times[role=red] and without role: icon:times[]";
        String expectedMarkdown = "This looks right with role: ✅ and without role: ✅ and this looks bad with role: ❌ and without role: ❌";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testDescriptionLists() {
        String asciidoc = """
                This is a simple paragraph.

                Item title1:: item description 1
                Item title2:: item description 2

                Item title1::
                Item description 1

                Item title2::
                Item description 2""";
        String expectedMarkdown = """
                This is a simple paragraph.

                **Item title1** item description 1
                **Item title2** item description 2

                **Item title1**
                Item description 1

                **Item title2**
                Item description 2""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }
}
