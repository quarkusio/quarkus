package io.quarkus.registry.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExtensionCatalogSerializationTest {
    static Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("src/test/resources/catalog-config");
    static Path writeDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-serialization");

    @BeforeAll
    static void verifyPath() throws IOException {
        Assertions.assertTrue(Files.exists(baseDir), baseDir + " should exist");
        Files.createDirectories(writeDir);
    }

    @Test
    public void testCatalogConfig() throws Exception {
        String fileName = "fake-catalog.json";

        List<String> codestarts = new ArrayList<>();
        codestarts.add(ArtifactCoords.pom("io.quarkus", "fake-artifact", "999-FAKE").toString());

        Map<String, Object> project = new HashMap<>();
        project.computeIfAbsent("properties", k -> {
            Map<String, Object> value = new HashMap<>();
            value.put("doc-root", "https://quarkus.io");
            value.put("rest-assured-version", "4.3.2");
            value.put("compiler-plugin-version", "3.8.1");
            value.put("surefire-plugin-version", "3.0.0-M5");
            value.put("kotlin-version", "1.4.31");
            value.put("scala-version", "2.12.13");
            value.put("scala-plugin-version", "4.4.0");
            value.put("quarkus-core-version", "999-FAKE");
            value.put("maven-plugin-groupId", "io.quarkus");
            value.put("maven-plugin-artifactId", "quarkus-maven-plugin");
            value.put("maven-plugin-version", "999-FAKE");
            value.put("gradle-plugin-id", "io.quarkus");
            value.put("gradle-plugin-version", "999-FAKE");
            value.put("supported-maven-versions", "[3.6.2,)");
            value.put("proposed-maven-version", "3.8.1");
            value.put("maven-wrapper-version", "0.7.7");
            value.put("gradle-wrapper-version", "7.2");
            return value;
        });

        ExtensionCatalog catalog = ExtensionCatalogImpl.builder()
                .withQuarkusCoreVersion("999-FAKE")
                .setMetadata("project", project)
                .setMetadata("codestarts-artifacts", codestarts)
                .build();

        assertDeserializedMatches(fileName, catalog);
        //assertSerializedMatches(catalog, fileName);
    }

    private void assertSerializedMatches(ExtensionCatalog catalog, String fileName) throws Exception {
        Path expectedPath = baseDir.resolve(fileName);
        Path actualPath = writeDir.resolve(fileName);
        CatalogMapperHelper.serialize(catalog, actualPath);

        Assertions.assertTrue(sameContents(expectedPath, actualPath),
                String.format("File %s does not have the expected content (%s)", actualPath, expectedPath));
    }

    private void assertDeserializedMatches(String fileName, ExtensionCatalog expected) throws Exception {
        ExtensionCatalog actual = CatalogMapperHelper.deserialize(baseDir.resolve(fileName), ExtensionCatalogImpl.class);
        assertThat(actual).isEqualTo(expected);
    }

    private boolean sameContents(Path expectedPath, Path actualPath) throws IOException {
        try (BufferedReader bf1 = Files.newBufferedReader(expectedPath);
                BufferedReader bf2 = Files.newBufferedReader(actualPath)) {
            String line1 = "", line2 = "";
            while ((line1 = bf1.readLine()) != null) {
                line2 = bf2.readLine();
                if (line2 == null || !line1.equals(line2)) {
                    return false; // files don't match (different content or length)
                }
            }
            return (bf2.readLine() == null)
                    ? true // files are identical
                    : false; // files have different length
        }
    }
}
