package io.quarkus.registry.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

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
    public void testPlatformsConfig() throws Exception {
        String fileName = "platforms.json";

        PlatformCatalog catalog = PlatformCatalog.builder()
                .addPlatform(Platform.builder()
                        .setPlatformKey("io.quarkus")
                        .addStream(PlatformStream.builder()
                                .setId("999-SNAPSHOT")
                                .addRelease(PlatformRelease.builder()
                                        .setQuarkusCoreVersion("999-SNAPSHOT")
                                        .setVersion(PlatformReleaseVersion.fromString("999-SNAPSHOT"))
                                        .setMemberBoms(Collections.singletonList(
                                                ArtifactCoords.fromString("io.quarkus:quarkus-bom::pom:999-SNAPSHOT")))
                                        .build())
                                .build())
                        .build())
                .build();

        PlatformCatalog deserialized = PlatformCatalog.fromFile(baseDir.resolve(fileName));
        assertThat(deserialized).isEqualTo(catalog);

        assertSerializedMatches(catalog, fileName);
    }

    @Test
    public void testExtensionCatalog() throws Exception {
        String fileName = "extension-catalog.json";

        Map<String, Object> project = new HashMap<>();
        project.computeIfAbsent("properties", k -> {
            Map<String, Object> value = new HashMap<>();
            value.put("doc-root", "https://quarkus.io");
            return value;
        });

        final ArtifactCoords bom = ArtifactCoords.pom("io.quarkus", "quarkus-bom", "999-FAKE");
        final ArtifactCoords fakeBom = ArtifactCoords.of("io.quarkus", "quarkus-fake-bom", "999-FAKE", "json", "999-FAKE");
        ExtensionCatalog catalog = ExtensionCatalog.builder()
                .setId(fakeBom.toString())
                .setBom(bom)
                .setPlatform(true)
                .setQuarkusCoreVersion("999-FAKE")
                .addExtension(Extension.builder()
                        .setName("RESTEasy Reactive")
                        .setDescription("Description")
                        .setArtifact(
                                ArtifactCoords.jar("io.quarkus", "quarkus-resteasy-reactive", "999-FAKE"))
                        .setOrigins(Arrays.asList(ExtensionOrigin.builder()
                                .setId(fakeBom.toString())))
                        .setMetadata("categories", Arrays.asList("web", "reactive")))
                .addCategory(Category.builder()
                        .setId("web")
                        .setName("Web")
                        .setDescription("Category description")
                        .setMetadata("pinned", Arrays.asList("blue", "green", "yellow")))
                .setMetadata("project", project)
                .build();

        ExtensionCatalog deserialized = ExtensionCatalog.fromFile(baseDir.resolve(fileName));
        assertThat(deserialized).isEqualTo(catalog);

        assertSerializedMatches(catalog, fileName);
    }

    private <T> void assertSerializedMatches(T source, String fileName) throws Exception {
        Path expectedPath = baseDir.resolve(fileName);
        Path actualPath = writeDir.resolve(fileName);
        CatalogMapperHelper.serialize(source, actualPath);

        Assertions.assertTrue(sameContents(actualPath, expectedPath),
                String.format("File %s does not have the expected content (%s)", actualPath, expectedPath));
    }

    private boolean sameContents(Path actualPath, Path expectedPath) throws IOException {
        try (BufferedReader bf1 = Files.newBufferedReader(expectedPath);
                BufferedReader bf2 = Files.newBufferedReader(actualPath)) {
            String line1 = "", line2 = "";
            while ((line1 = bf1.readLine()) != null) {
                line2 = bf2.readLine();
                if (line2 == null || !line1.equals(line2)) {
                    return false; // files don't match (different content or length)
                }
            }
            return bf2.readLine() == null; // return true if both files end
        }
    }
}
