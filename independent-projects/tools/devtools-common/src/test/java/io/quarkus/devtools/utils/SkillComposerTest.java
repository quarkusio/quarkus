package io.quarkus.devtools.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SkillComposerTest {

    @Test
    public void outputSkillPathProducesCorrectLayout() {
        assertEquals("META-INF/skills/quarkus-arc/SKILL.md",
                SkillComposer.outputSkillPath("quarkus-arc"));
    }

    @Test
    public void parseExtensionMetadataReadsYaml() throws IOException {
        String yaml = "name: \"My Extension\"\ndescription: \"Does things\"\n";
        try (var is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            ObjectNode node = SkillComposer.parseExtensionMetadata(is);
            assertNotNull(node);
            assertEquals("My Extension", node.path("name").asText());
            assertEquals("Does things", node.path("description").asText());
        }
    }

    @Test
    public void parseExtensionMetadataThrowsOnEmptyStream() {
        // Empty YAML should throw
        assertThrows(Exception.class, () -> {
            try (var is = new ByteArrayInputStream(new byte[0])) {
                SkillComposer.parseExtensionMetadata(is);
            }
        });
    }

    @Test
    public void composeWithFullMetadata() throws IOException {
        String yaml = "name: \"REST\"\n"
                + "description: \"Build RESTful APIs\"\n"
                + "metadata:\n"
                + "  guide: https://quarkus.io/guides/rest\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "### Endpoints\n- Use @GET\n", "quarkus-rest");

        assertTrue(result.startsWith("---\n"));
        assertTrue(result.contains("name: \"quarkus-rest\"\n"));
        assertTrue(result.contains("description: \"Build RESTful APIs\"\n"));
        assertTrue(result.contains("license: \"Apache-2.0\"\n"));
        assertTrue(result.contains("  guide: \"https://quarkus.io/guides/rest\"\n"));
        assertTrue(result.contains("### Endpoints"));
        assertTrue(result.contains("- Use @GET"));
        // Ends with closing frontmatter separator, blank line, then body
        assertTrue(result.contains("---\n\n### Endpoints"));
    }

    @Test
    public void composeWithoutDescriptionOmitsField() throws IOException {
        String yaml = "name: \"Minimal\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "content", "quarkus-minimal");

        assertTrue(result.contains("name: \"quarkus-minimal\"\n"));
        assertTrue(!result.contains("description:"));
        assertTrue(result.contains("license: \"Apache-2.0\"\n"));
    }

    @Test
    public void composeWithBlankDescriptionOmitsField() throws IOException {
        String yaml = "name: \"Blank\"\ndescription: \"   \"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "content", "quarkus-blank");

        assertTrue(!result.contains("description:"));
    }

    @Test
    public void composeWithoutGuideOrCategoriesOmitsMetadataBlock() throws IOException {
        String yaml = "name: \"No Guide\"\ndescription: \"Something\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "quarkus-noguide");

        assertFalse(result.contains("metadata:"));
        assertFalse(result.contains("guide:"));
        assertFalse(result.contains("categories:"));
    }

    @Test
    public void composeWithCategoriesIncludesThem() throws IOException {
        String yaml = "name: \"REST\"\n"
                + "description: \"Build RESTful APIs\"\n"
                + "metadata:\n"
                + "  guide: https://quarkus.io/guides/rest\n"
                + "  categories:\n"
                + "  - \"web\"\n"
                + "  - \"reactive\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "quarkus-rest");

        assertTrue(result.contains("metadata:\n"));
        assertTrue(result.contains("  guide: \"https://quarkus.io/guides/rest\"\n"));
        assertTrue(result.contains("  categories: \"web, reactive\"\n"));
    }

    @Test
    public void composeWithCategoriesOnlyNoGuide() throws IOException {
        String yaml = "name: \"Ext\"\n"
                + "description: \"desc\"\n"
                + "metadata:\n"
                + "  categories:\n"
                + "  - \"data\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "quarkus-ext");

        assertTrue(result.contains("metadata:\n"));
        assertFalse(result.contains("guide:"));
        assertTrue(result.contains("  categories: \"data\"\n"));
    }

    @Test
    public void composeWithoutCategoriesOmitsThem() throws IOException {
        String yaml = "name: \"No Cat\"\ndescription: \"Something\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "quarkus-nocat");

        assertFalse(result.contains("categories:"));
    }

    @Test
    public void composeEscapesQuotesInDescription() throws IOException {
        String yaml = "name: \"Quoted\"\ndescription: 'Say \"hello\" world'\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "quarkus-quoted");

        assertTrue(result.contains("description: \"Say \\\"hello\\\" world\"\n"));
    }

    @Test
    public void composeTrimsRawContent() throws IOException {
        String yaml = "name: \"Trim\"\ndescription: \"desc\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "\n  content with whitespace  \n\n", "quarkus-trim");

        // Body should be trimmed and end with single newline
        assertTrue(result.endsWith("content with whitespace\n"));
    }

    @Test
    public void composeWithCustomLicense() throws IOException {
        String yaml = "name: \"Custom\"\ndescription: \"desc\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "my-ext", "MIT");

        assertTrue(result.contains("license: \"MIT\"\n"));
        assertTrue(!result.contains("Apache-2.0"));
    }

    @Test
    public void composeWithNullLicenseOmitsField() throws IOException {
        String yaml = "name: \"NoLicense\"\ndescription: \"desc\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "my-ext", null);

        assertTrue(!result.contains("license:"));
    }

    @Test
    public void composeIgnoresNonTextualDescription() throws IOException {
        // description is an object, not a string — should be treated as missing
        String yaml = "name: \"Bad\"\ndescription:\n  nested: value\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "quarkus-bad");

        assertTrue(!result.contains("description:"));
    }

    @Test
    public void composeEscapesNonPrintableInDescription() throws IOException {
        // Use a description containing a null byte and a control character
        String yaml = "name: \"Esc\"\ndescription: \"has ctrl\"\n";
        ObjectNode meta = parseYaml(yaml);
        // Manually set description with non-printable chars
        meta.put("description", "before\0after\u0007bell");

        String result = SkillComposer.compose(meta, "body", "quarkus-esc");

        assertTrue(result.contains("description: \"before\\0after\\u0007bell\"\n"));
    }

    @Test
    public void composeQuotesNameWithSpecialChars() throws IOException {
        String yaml = "name: \"Special\"\ndescription: \"desc\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.compose(meta, "body", "my-ext: special");

        assertTrue(result.contains("name: \"my-ext: special\"\n"));
    }

    @Test
    public void composeThrowsOnNullArgs() throws IOException {
        ObjectNode meta = parseYaml("name: x\n");

        assertThrows(NullPointerException.class, () -> SkillComposer.compose(null, "c", "n"));
        assertThrows(NullPointerException.class, () -> SkillComposer.compose(meta, null, "n"));
        assertThrows(NullPointerException.class, () -> SkillComposer.compose(meta, "c", null));
    }

    @Test
    public void formatMcpToolsSectionProducesValidTable() {
        Map<String, SkillComposer.ParameterInfo> params = new LinkedHashMap<>();
        params.put("identity", new SkillComposer.ParameterInfo("The job ID", true));

        List<SkillComposer.McpToolInfo> tools = List.of(
                new SkillComposer.McpToolInfo("getData", "Get scheduler info", null),
                new SkillComposer.McpToolInfo("pauseJob", "Pause a specific job", params));

        String result = SkillComposer.formatMcpToolsSection(tools, "quarkus-scheduler");

        assertTrue(result.startsWith("### Available Dev MCP Tools\n\n"));
        assertTrue(result.contains("| `quarkus-scheduler_getData` | Get scheduler info | \u2014 |"));
        assertTrue(result.contains("| `quarkus-scheduler_pauseJob` | Pause a specific job | "
                + "`identity` (required): The job ID |"));
    }

    @Test
    public void composeWithToolsIncludesToolsSection() throws IOException {
        String yaml = "name: \"Sched\"\ndescription: \"Job scheduling\"\n";
        ObjectNode meta = parseYaml(yaml);

        List<SkillComposer.McpToolInfo> tools = List.of(
                new SkillComposer.McpToolInfo("getData", "Get info", null));

        String result = SkillComposer.composeWithTools(meta, "### Usage\n- Use @Scheduled", "quarkus-scheduler", tools);

        assertTrue(result.contains("### Usage"));
        assertTrue(result.contains("### Available Dev MCP Tools"));
        assertTrue(result.contains("| `quarkus-scheduler_getData` | Get info |"));
    }

    @Test
    public void composeWithEmptyToolsOmitsSection() throws IOException {
        String yaml = "name: \"Ext\"\ndescription: \"desc\"\n";
        ObjectNode meta = parseYaml(yaml);

        String result = SkillComposer.composeWithTools(meta, "body", "quarkus-ext", List.of());

        assertFalse(result.contains("Available Dev MCP Tools"));
    }

    @Test
    public void formatMcpToolsEscapesPipeInDescription() {
        List<SkillComposer.McpToolInfo> tools = List.of(
                new SkillComposer.McpToolInfo("method", "Returns A | B", null));

        String result = SkillComposer.formatMcpToolsSection(tools, "ext");

        assertTrue(result.contains("Returns A \\| B"));
    }

    private static ObjectNode parseYaml(String yaml) throws IOException {
        try (var is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            return SkillComposer.parseExtensionMetadata(is);
        }
    }
}
