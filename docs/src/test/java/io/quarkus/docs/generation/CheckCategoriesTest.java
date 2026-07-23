package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CheckCategoriesTest {

    @TempDir
    Path tempDir;

    @Test
    public void shouldReportGuidesReferencedInCategoriesButMissingFromSourceTree() throws Exception {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("existing.adoc"), "= Existing\n");

        Path categoriesFile = tempDir.resolve("categories.yaml");
        Files.writeString(categoriesFile, String.join("\n",
                "categories:",
                "  - id: web",
                "    title: Web",
                "    guides:",
                "      - existing.adoc",
                "      - missing.adoc",
                ""));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> CheckCategories.main(new String[] { srcDir.toString(), categoriesFile.toString() }));

        assertTrue(exception.getMessage().contains("missing.adoc"));
        assertTrue(exception.getMessage().contains("referenced in categories.yaml but do not exist"));
    }
}
