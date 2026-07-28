package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InjectCategoriesTest {

    @TempDir
    Path tempDir;

    @Test
    public void shouldOnlyInjectCategoriesInDocumentHeader() throws Exception {
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

        InjectCategories.injectAttributes(guide, "contributing", "contributing.docs");

        List<String> lines = Files.readAllLines(guide);
        assertEquals(":categories: contributing", lines.get(3));
        assertEquals(":categories-path: contributing.docs", lines.get(4));
        assertEquals(":categories: web // <4>", lines.get(8));
    }

    @Test
    public void shouldInsertCategoriesAfterAttributesInclude() throws Exception {
        Path guide = tempDir.resolve("guide.adoc");
        Files.writeString(guide, String.join("\n",
                "[id=\"guide\"]",
                "= Guide",
                "include::_attributes.adoc[]",
                "",
                "Content",
                ""));

        InjectCategories.injectAttributes(guide, "web", "web.web-rest");

        List<String> lines = Files.readAllLines(guide);
        assertEquals(":categories: web", lines.get(3));
        assertEquals(":categories-path: web.web-rest", lines.get(4));
    }

    @Test
    public void shouldBuildFlatCategoriesAndCategoryPathsFromHierarchy() throws Exception {
        Path categoriesFile = tempDir.resolve("categories.yaml");
        Files.writeString(categoriesFile, String.join("\n",
                "categories:",
                "  - id: web",
                "    title: Web",
                "    subcategories:",
                "      - id: web-rest",
                "        title: REST",
                "        guides:",
                "          - rest-json.adoc",
                "  - id: serialization",
                "    title: Serialization",
                "    subcategories:",
                "      - id: serialization-json",
                "        title: JSON",
                "        guides:",
                "          - rest-json.adoc",
                ""));

        Map<String, InjectCategories.GuideCategories> categories = InjectCategories.buildGuideToCategories(categoriesFile);

        InjectCategories.GuideCategories guideCategories = categories.get("rest-json.adoc");
        assertEquals("web,serialization", guideCategories.categoriesValue());
        assertEquals("web.web-rest,serialization.serialization-json", guideCategories.pathsValue());
    }

    @Test
    public void generatedCategoriesShouldBeKnownToYamlMetadataGenerator() throws Exception {
        Path categoriesFile = Path.of("src/main/resources/categories.yaml");
        if (!Files.exists(categoriesFile)) {
            categoriesFile = Path.of("docs/src/main/resources/categories.yaml");
        }

        Set<String> metadataCategories = Arrays.stream(YamlMetadataGenerator.Category.values())
                .map(category -> category.id)
                .collect(Collectors.toSet());

        Set<String> generatedCategories = InjectCategories.buildGuideToCategories(categoriesFile).values().stream()
                .flatMap(categories -> Arrays.stream(categories.categoriesValue().split(",")))
                .collect(Collectors.toSet());

        generatedCategories.removeAll(metadataCategories);
        assertTrue(generatedCategories.isEmpty(),
                "categories.yaml contains categories that YamlMetadataGenerator does not accept: " + generatedCategories);
    }
}
