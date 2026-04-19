package io.quarkus.annotation.processor.documentation.config.formatter;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
 
import org.junit.jupiter.api.Test;
 
import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
 
public class JavadocToMarkdownTransformerTest {
 
    @Test
    public void testAsciidocToMarkdown() {
        String asciidoc = "*Bold text* and _italics_ with `monospace`.\n" +
                "link:https://quarkus.io[Quarkus] website.\n" +
                "`simple`::\n" +
                "This is a definition.";
        
        String expected = "**Bold text** and *italics* with `monospace`.\n" +
                "[Quarkus](https://quarkus.io) website.\n" +
                "**simple**:\n" +
                "This is a definition.";
        
        assertEquals(expected, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }
 
    @Test
    public void testTableConversion() {
        String asciidoc = "!===\n" +
                "h!Header\n" +
                "!Cell 1\n" +
                "!===";
        
        String expected = "<table>\n" +
                "  <tr><th>Header</th></tr>\n" +
                "  <tr><td>Cell 1</td></tr>\n" +
                "</table>";
        
        assertEquals(expected, JavadocToMarkdownTransformer.toMarkdown(asciidoc, JavadocFormat.ASCIIDOC));
    }
}
