package io.quarkus.devtools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

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
        final String categories = getNestedArrayAsString(extMeta, "metadata", "categories");
        if ((guide != null && !guide.isBlank()) || (categories != null && !categories.isBlank())) {
            sb.append("metadata:\n");
            if (guide != null && !guide.isBlank()) {
                sb.append("  guide: \"").append(escapeYamlString(guide)).append("\"\n");
            }
            if (categories != null && !categories.isBlank()) {
                sb.append("  categories: \"").append(escapeYamlString(categories)).append("\"\n");
            }
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
     * Composes a skill document and appends an "Available Dev MCP Tools" section
     * listing the extension's MCP-enabled methods.
     *
     * @param extMeta the parsed {@code quarkus-extension.yaml}
     * @param rawContent the raw skill file content
     * @param skillName the skill identifier
     * @param mcpTools the MCP tools to include; if empty, no tools section is appended
     */
    public static String composeWithTools(ObjectNode extMeta, String rawContent, String skillName,
            List<McpToolInfo> mcpTools) {
        String enrichedContent = rawContent;
        if (mcpTools != null && !mcpTools.isEmpty()) {
            enrichedContent = rawContent.trim() + "\n\n" + formatMcpToolsSection(mcpTools, skillName);
        }
        return compose(extMeta, enrichedContent, skillName, "Apache-2.0");
    }

    /**
     * Formats a markdown table listing Dev MCP tools for inclusion in a skill file.
     *
     * @param tools the MCP tools to format
     * @param extensionName the extension name used as a prefix for fully qualified tool names
     */
    public static String formatMcpToolsSection(List<McpToolInfo> tools, String extensionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Available Dev MCP Tools\n\n");
        sb.append("| Tool | Description | Parameters |\n");
        sb.append("|------|-------------|------------|\n");

        for (McpToolInfo tool : tools) {
            String fullName = extensionName + "_" + tool.name();
            sb.append("| `").append(fullName).append("` | ");
            sb.append(escapeMarkdownTable(tool.description())).append(" | ");

            if (tool.parameters() != null && !tool.parameters().isEmpty()) {
                StringJoiner pj = new StringJoiner(", ");
                for (Map.Entry<String, ParameterInfo> param : tool.parameters().entrySet()) {
                    StringBuilder ps = new StringBuilder();
                    ps.append("`").append(param.getKey()).append("`");
                    if (param.getValue().required()) {
                        ps.append(" (required)");
                    }
                    if (param.getValue().description() != null) {
                        ps.append(": ").append(escapeMarkdownTable(param.getValue().description()));
                    }
                    pj.add(ps.toString());
                }
                sb.append(pj);
            } else {
                sb.append("\u2014");
            }
            sb.append(" |\n");
        }

        return sb.toString();
    }

    /**
     * Describes an MCP tool discovered from an extension's annotated methods.
     * Intentionally not a record — this class must be usable from modules targeting JDK 11.
     */
    public static final class McpToolInfo {
        private final String name;
        private final String description;
        private final Map<String, ParameterInfo> parameters;

        public McpToolInfo(String name, String description, Map<String, ParameterInfo> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }

        public Map<String, ParameterInfo> parameters() {
            return parameters;
        }
    }

    /**
     * Describes a parameter of an MCP tool.
     * Intentionally not a record — this class must be usable from modules targeting JDK 11.
     */
    public static final class ParameterInfo {
        private final String description;
        private final boolean required;

        public ParameterInfo(String description, boolean required) {
            this.description = description;
            this.required = required;
        }

        public String description() {
            return description;
        }

        public boolean required() {
            return required;
        }
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

    /**
     * Extracts a nested array (e.g. {@code metadata.categories}) and joins its text
     * elements with {@code ", "}, returning {@code null} if the path is missing or empty.
     */
    private static String getNestedArrayAsString(ObjectNode node, String parent, String field) {
        var parentNode = node.get(parent);
        if (parentNode == null || parentNode.isNull() || !parentNode.isObject()) {
            return null;
        }
        var arrayNode = parentNode.get(field);
        if (arrayNode == null || arrayNode.isNull() || !arrayNode.isArray() || arrayNode.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (var element : arrayNode) {
            if (element.isTextual()) {
                joiner.add(element.asText());
            }
        }
        String result = joiner.toString();
        return result.isEmpty() ? null : result;
    }

    private static String escapeMarkdownTable(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
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
