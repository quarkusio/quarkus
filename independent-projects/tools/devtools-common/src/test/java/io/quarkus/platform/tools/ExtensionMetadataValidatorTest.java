package io.quarkus.platform.tools;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ExtensionMetadataValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsValidTemplateDescriptor() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.put("artifact", "com.example:my-extension:1.0.0");
        ObjectNode metadata = extObject.putObject("metadata");
        metadata.put("status", "deprecated");
        ArrayNode keywords = metadata.putArray("keywords");
        keywords.add("example");
        ArrayNode categories = metadata.putArray("categories");
        categories.add("integration");

        assertDoesNotThrow(() -> ExtensionMetadataValidator.validate(extObject));
    }

    @Test
    void acceptsValidBuildAugmentedDescriptor() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.put("description", "Does something useful");
        extObject.put("scm-url", "https://github.com/example/my-extension");
        ObjectNode metadata = extObject.putObject("metadata");
        metadata.put("status", "stable");
        metadata.put("built-with-quarkus-core", "3.27.0");
        metadata.put("requires-quarkus-core", "[3.27,)");
        metadata.put("minimum-java-version", "17");
        metadata.putArray("extension-dependencies").add("io.quarkus:quarkus-arc");

        assertDoesNotThrow(() -> ExtensionMetadataValidator.validate(extObject));
    }

    @Test
    void rejectsStatusArray() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        ArrayNode status = metadata.putArray("status");
        status.add("stable");
        status.add("deprecated");

        Exception thrown = assertThrows(Exception.class, () -> ExtensionMetadataValidator.validate(extObject));
        assertTrue(thrown.getMessage().contains("metadata/status"), thrown.getMessage());
    }

    @Test
    void rejectsInvalidStatusValue() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("status", "retired");

        Exception thrown = assertThrows(Exception.class, () -> ExtensionMetadataValidator.validate(extObject));
        assertTrue(thrown.getMessage().contains("metadata/status"), thrown.getMessage());
    }

    @Test
    void rejectsKeywordsAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("keywords", "web");

        Exception thrown = assertThrows(Exception.class, () -> ExtensionMetadataValidator.validate(extObject));
        assertTrue(thrown.getMessage().contains("metadata/keywords"), thrown.getMessage());
    }
}
