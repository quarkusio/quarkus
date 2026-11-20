package io.quarkus.annotation.processor.documentation.config.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

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
    public void testAttributes() {
        Map<String, String> attributes = Map.of(
                "hibernate-search-docs-url", "https://docs.jboss.org/hibernate/search/7.1/reference/en-US/html_single/");

        String asciidoc = "link:{hibernate-search-docs-url}#backend-elasticsearch-configuration-index-settings[this section of the reference documentation]";
        String expectedMarkdown = "[this section of the reference documentation](https://docs.jboss.org/hibernate/search/7.1/reference/en-US/html_single/#backend-elasticsearch-configuration-index-settings)";
        assertEquals(expectedMarkdown,
                JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC, attributes));

        asciidoc = "Set it to {hibernate-search-docs-url} or keep {unknown-attribute} as is.";
        expectedMarkdown = "Set it to https://docs.jboss.org/hibernate/search/7.1/reference/en-US/html_single/ or keep {unknown-attribute} as is.";
        assertEquals(expectedMarkdown,
                JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC, attributes));
    }

    @Test
    public void testLabeledLists() {
        String asciidoc = """
                `simple`::
                The default, future-proof strategy: if the index name in Hibernate Search is `myIndex`,
                this strategy will create an index named `myindex-000001`.
                `no-alias`::
                A strategy without index aliases.""";
        String expectedMarkdown = """
                - **`simple`**:
                  The default, future-proof strategy: if the index name in Hibernate Search is `myIndex`,
                  this strategy will create an index named `myindex-000001`.
                - **`no-alias`**:
                  A strategy without index aliases.""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testLabeledListInlineDefinition() {
        String asciidoc = "`async`:: Changes applied when the application thread resumes";
        String expectedMarkdown = "- **`async`**: Changes applied when the application thread resumes";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testIcons() {
        String asciidoc = "icon:check[role=lime] icon:times[role=red]";
        String expectedMarkdown = "✔️ ✖️";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testSimpleTable() {
        String asciidoc = """
                |===
                |Name |Type

                |async
                |boolean
                |===""";
        String expectedMarkdown = """
                <table>
                <tr><td>Name</td><td>Type</td></tr>
                <tr><td>async</td><td>boolean</td></tr>
                </table>""";

        assertEquals(expectedMarkdown, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }

    @Test
    public void testComplexTable() {
        // taken from the Hibernate Search configuration reference
        String asciidoc = """
                [cols=5]
                !===
                .2+h!Strategy
                .2+h!Throughput
                3+^h!Guarantees when the application thread resumes

                h!Changes applied
                h!Changes safe from crash/power loss
                h!Changes visible on search

                !async
                !Best
                ^!icon:times[role=red]
                ^!icon:times[role=red]
                ^!icon:times[role=red]

                !sync
                !Worst
                ^!icon:check[role=lime]
                ^!icon:check[role=lime]
                ^!icon:check[role=lime]
                !===""";
        String expectedMarkdown = """
                <table>
                <tr><th rowspan="2">Strategy</th><th rowspan="2">Throughput</th><th colspan="3" align="center">Guarantees when the application thread resumes</th></tr>
                <tr><th>Changes applied</th><th>Changes safe from crash/power loss</th><th>Changes visible on search</th></tr>
                <tr><td>async</td><td>Best</td><td align="center">✖️</td><td align="center">✖️</td><td align="center">✖️</td></tr>
                <tr><td>sync</td><td>Worst</td><td align="center">✔️</td><td align="center">✔️</td><td align="center">✔️</td></tr>
                </table>""";

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
}
