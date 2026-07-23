package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InjectCategoriesTest {

    @TempDir
    Path tempDir;

    @Test
    public void shouldOnlyInjectCategoryPathInDocumentHeader() throws Exception {
        Path guide = tempDir.resolve("guide.adoc");
        Files.writeString(guide, String.join("\n",
                "[id=\"guide\"]",
                "= Guide",
                "include::_attributes.adoc[]",
                ":categories: data",
                "",
                "[source,asciidoc]",
                "----",
                ":categories: web // <4>",
                "----",
                ""));

        InjectCategories.injectAttribute(guide, "contributing.docs");

        List<String> lines = Files.readAllLines(guide);
        assertEquals(":categories: data", lines.get(3));
        assertEquals(":categories-path: contributing.docs", lines.get(4));
        assertEquals(":categories: web // <4>", lines.get(8));
    }

    @Test
    public void shouldInsertCategoryPathAfterAttributesInclude() throws Exception {
        Path guide = tempDir.resolve("guide.adoc");
        Files.writeString(guide, String.join("\n",
                "[id=\"guide\"]",
                "= Guide",
                "include::_attributes.adoc[]",
                "",
                "Content",
                ""));

        InjectCategories.injectAttribute(guide, "web.web-rest");

        List<String> lines = Files.readAllLines(guide);
        assertEquals(":categories-path: web.web-rest", lines.get(3));
    }
}
