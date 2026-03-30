package io.quarkus.devtools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Shared utility for composing skill files with extension metadata,
 * following the <a href="https://agentskills.io/specification">Agent Skills</a> format.
 * Used by both the Maven Mojo and the Gradle task.
 */
public final class SkillComposer {

    /** Path where extension developers place the raw skill source in deployment modules. */
    public static final String SOURCE_SKILL_PATH = "META-INF/quarkus-skill.md";

    public static final String EXTENSION_METADATA_PATH = "META-INF/quarkus-extension.yaml";

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

    private SkillComposer() {
    }

    /**
     * Returns the output path for a composed skill file inside the deployment JAR,
     * following the Agent Skills directory convention.
     *
     * @param skillName the skill identifier (e.g. {@code quarkus-arc})
     */
    public static String outputSkillPath(String skillName) {
        return "META-INF/skills/" + skillName + "/SKILL.md";
    }

    /**
     * Parses a {@code quarkus-extension.yaml} from the given input stream.
     *
     * @throws IOException if the stream cannot be read, the YAML is malformed, or the file is empty
     */
    public static ObjectNode parseExtensionMetadata(InputStream is) throws IOException {
        final ObjectNode result = YAML_MAPPER.readValue(is, ObjectNode.class);
        if (result == null) {
            throw new IOException("quarkus-extension.yaml is empty or contains no object");
        }
        return result;
    }

    /**
     * Composes a skill document from extension metadata and raw skill content,
     * producing a {@code SKILL.md} file with YAML frontmatter per the
     * <a href="https://agentskills.io/specification">Agent Skills specification</a>.
     *
     * @param extMeta the parsed {@code quarkus-extension.yaml} as an {@link ObjectNode}
     * @param rawContent the raw skill file content authored by the extension developer
     * @param skillName the skill identifier used in the frontmatter {@code name} field
     *        (e.g. {@code quarkus-arc}, derived from the runtime artifact name)
     * @param license the SPDX license identifier (e.g. {@code Apache-2.0}), may be {@code null}
     */
    public static String compose(ObjectNode extMeta, String rawContent, String skillName, String license) {
        Objects.requireNonNull(extMeta, "extMeta must not be null");
        Objects.requireNonNull(rawContent, "rawContent must not be null");
        Objects.requireNonNull(skillName, "skillName must not be null");

        final StringBuilder sb = new StringBuilder();

        // YAML frontmatter
        sb.append("---\n");
        sb.append("name: \"").append(escapeYamlString(skillName)).append("\"\n");

        final String description = getTextValue(extMeta, "description");
        if (description != null && !description.isBlank()) {
            sb.append("description: \"").append(escapeYamlString(description)).append("\"\n");
        }

        if (license != null && !license.isBlank()) {
            sb.append("license: \"").append(escapeYamlString(license)).append("\"\n");
        }

        final String guide = getNestedTextValue(extMeta, "metadata", "guide");
        if (guide != null && !guide.isBlank()) {
            sb.append("metadata:\n");
            sb.append("  guide: \"").append(escapeYamlString(guide)).append("\"\n");
        }

        sb.append("---\n\n");

        // Skill body
        sb.append(rawContent.trim()).append("\n");

        return sb.toString();
    }

    /**
     * Overload that defaults to {@code Apache-2.0} license for Quarkus core extensions.
     */
    public static String compose(ObjectNode extMeta, String rawContent, String skillName) {
        return compose(extMeta, rawContent, skillName, "Apache-2.0");
    }

    /**
     * Safely extracts a text value from a JSON node, returning {@code null} if
     * the field is missing, null, or not a textual type.
     */
    private static String getTextValue(ObjectNode node, String field) {
        var child = node.get(field);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }
        return child.asText();
    }

    /**
     * Safely extracts a nested text value (e.g. {@code metadata.guide}),
     * returning {@code null} if any part of the path is missing or not the expected type.
     */
    private static String getNestedTextValue(ObjectNode node, String parent, String field) {
        var parentNode = node.get(parent);
        if (parentNode == null || parentNode.isNull() || !parentNode.isObject()) {
            return null;
        }
        return getTextValue((ObjectNode) parentNode, field);
    }

    private static String escapeYamlString(String value) {
        final StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\0':
                    sb.append("\\0");
                    break;
                default:
                    if (c < 0x20) {
                        // Escape non-printable control characters as Unicode
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
