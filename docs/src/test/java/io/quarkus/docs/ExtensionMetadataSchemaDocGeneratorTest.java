package io.quarkus.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.docs.generation.ExtensionMetadataSchemaDocGenerator;

public class ExtensionMetadataSchemaDocGeneratorTest {

    @Test
    public void generatesReferenceFromCanonicalSchema() throws Exception {
        Path schemaFile = Path.of("..", "independent-projects", "tools", "devtools-common", "src", "main", "resources",
                "META-INF", "quarkus-extension-schema.json");
        Path outputFile = Path.of("target", "test-generated", "quarkus-extension-schema-reference.adoc");

        new ExtensionMetadataSchemaDocGenerator().generate(outputFile, schemaFile);

        String generated = Files.readString(outputFile);
        assertTrue(generated.contains("quarkus-extension-schema-reference"));
        assertTrue(generated.contains("metadata.status"));
        assertTrue(generated.contains("`deprecated`"));
        assertTrue(generated.contains("metadata.capabilities.provides"));
        assertTrue(generated.contains("Author template fields"));
        assertTrue(generated.contains("Build-augmented fields"));
        assertTrue(generated.contains("Extension offering fields"));
        assertTrue(generated.contains("https://quarkus.io/schemas/quarkus-extension-schema.json"));
    }
}
