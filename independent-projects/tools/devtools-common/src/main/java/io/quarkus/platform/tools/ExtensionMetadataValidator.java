package io.quarkus.platform.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * Validates extension metadata ({@value BootstrapConstants#QUARKUS_EXTENSION_FILE_NAME}) against the
 * JSON schema bundled in {@value ToolsConstants#EXTENSION_SCHEMA_RESOURCE}.
 * <p>
 * This class is shared by the extension Maven plugin and the extension Gradle plugin so that the
 * validation logic is not duplicated.
 */
public final class ExtensionMetadataValidator {

    private ExtensionMetadataValidator() {
    }

    /**
     * Validates the given extension descriptor object against the bundled JSON schema.
     *
     * @param extObject the parsed extension descriptor
     * @throws IOException if the schema cannot be loaded or the descriptor is invalid
     */
    public static void validate(ObjectNode extObject) throws IOException {
        final Schema schema;
        try (InputStream is = ExtensionMetadataValidator.class.getResourceAsStream(ToolsConstants.EXTENSION_SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new IOException(
                        "Failed to load extension metadata schema from " + ToolsConstants.EXTENSION_SCHEMA_RESOURCE);
            }
            final ObjectMapper jsonMapper = JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                    .build();
            final JsonNode schemaNode = jsonMapper.readTree(is);

            final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
            schema = schemaRegistry.getSchema(schemaNode);
            schema.initializeValidators();
        }

        final List<com.networknt.schema.Error> errors = schema.validate(extObject);
        if (!errors.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Invalid ").append(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME).append(" metadata:");
            for (com.networknt.schema.Error err : errors) {
                sb.append(System.lineSeparator()).append("- ").append(err.getInstanceLocation()).append(": ")
                        .append(err.getMessage());
            }
            throw new IOException(sb.toString());
        }
    }
}
