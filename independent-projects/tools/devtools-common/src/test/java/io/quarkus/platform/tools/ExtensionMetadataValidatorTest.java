package io.quarkus.platform.tools;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;

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
        extObject.put("description", "Does something useful");
        ObjectNode metadata = extObject.putObject("metadata");
        metadata.put("short-name", "my-ext");
        metadata.put("status", "deprecated");
        metadata.put("guide", "https://quarkus.io/guides/my-extension");
        metadata.put("icon-url", "https://example.com/icon.svg");
        metadata.putArray("keywords").add("example");
        metadata.putArray("categories").add("integration");
        metadata.putArray("config").add("quarkus.my-extension.");
        ObjectNode codestart = metadata.putObject("codestart");
        codestart.put("name", "my-extension");
        codestart.put("artifact", "io.quarkus:quarkus-project-core-extension-codestarts");
        codestart.putArray("languages").add("java").add("kotlin");

        assertThatCode(() -> ExtensionMetadataValidator.validate(extObject)).doesNotThrowAnyException();
    }

    @Test
    void acceptsValidBuildAugmentedDescriptor() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.put("description", "Does something useful");
        extObject.put("scm-url", "https://github.com/example/my-extension");
        extObject.put("sponsor", "Example Org");
        ObjectNode metadata = extObject.putObject("metadata");
        metadata.put("status", "stable");
        metadata.put("built-with-quarkus-core", "3.27.0");
        metadata.put("requires-quarkus-core", "[3.27,)");
        metadata.put("minimum-java-version", "17");
        metadata.putArray("extension-dependencies").add("io.quarkus:quarkus-arc");
        ObjectNode capabilities = metadata.putObject("capabilities");
        capabilities.putArray("provides").add("io.quarkus.example");
        capabilities.putArray("requires").add("io.quarkus.arc");

        assertThatCode(() -> ExtensionMetadataValidator.validate(extObject)).doesNotThrowAnyException();
    }

    @Test
    void acceptsValidOfferingField() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        metadata.put("status", "stable");
        metadata.putArray("acme-cloud-support").add("supported");
        metadata.putArray("team-x-support").add("tech-preview");

        assertThatCode(() -> ExtensionMetadataValidator.validate(extObject)).doesNotThrowAnyException();
    }

    @Test
    void acceptsMinimalDescriptor() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("status", "stable");

        assertThatCode(() -> ExtensionMetadataValidator.validate(extObject)).doesNotThrowAnyException();
    }

    @Test
    void acceptsUndocumentedMetadataFields() throws Exception {
        ObjectNode extObject;
        try (InputStream is = getClass().getResourceAsStream("/sample-quarkus-extension-template.json")) {
            extObject = (ObjectNode) mapper.readTree(is);
        }

        assertThatCode(() -> ExtensionMetadataValidator.validate(extObject)).doesNotThrowAnyException();
    }

    @Test
    void acceptsUnknownMetadataFields() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        metadata.put("status", "stable");
        metadata.put("future-field", "allowed");

        assertThatCode(() -> ExtensionMetadataValidator.validate(extObject)).doesNotThrowAnyException();
    }

    @Test
    void rejectsStatusArray() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        ArrayNode status = metadata.putArray("status");
        status.add("stable");
        status.add("deprecated");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/status");
    }

    @Test
    void rejectsInvalidStatusValue() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("status", "retired");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/status");
    }

    @Test
    void rejectsKeywordsAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("keywords", "web");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/keywords");
    }

    @Test
    void rejectsCategoriesAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("categories", "web");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/categories");
    }

    @Test
    void rejectsConfigAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("config", "quarkus.example.");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/config");
    }

    @Test
    void rejectsCodestartLanguagesAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        ObjectNode codestart = metadata.putObject("codestart");
        codestart.put("name", "my-extension");
        codestart.put("languages", "java");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/codestart/languages");
    }

    @Test
    void rejectsCapabilitiesProvidesAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        ObjectNode capabilities = metadata.putObject("capabilities");
        capabilities.put("provides", "io.quarkus.example");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/capabilities/provides");
    }

    @Test
    void rejectsCapabilitiesRequiresAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        ObjectNode metadata = extObject.putObject("metadata");
        ObjectNode capabilities = metadata.putObject("capabilities");
        capabilities.put("requires", "io.quarkus.arc");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/capabilities/requires");
    }

    @Test
    void rejectsOfferingFieldAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("acme-cloud-support", "supported");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/acme-cloud-support");
    }

    @Test
    void rejectsExtensionDependenciesAsString() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("extension-dependencies", "io.quarkus:quarkus-arc");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("metadata/extension-dependencies");
    }

    @Test
    void errorMessageIncludesDescriptorFileName() {
        ObjectNode extObject = mapper.createObjectNode();
        extObject.put("name", "My Extension");
        extObject.putObject("metadata").put("status", "retired");

        assertThatThrownBy(() -> ExtensionMetadataValidator.validate(extObject))
                .hasMessageContaining("Invalid quarkus-extension.yaml metadata");
    }
}
