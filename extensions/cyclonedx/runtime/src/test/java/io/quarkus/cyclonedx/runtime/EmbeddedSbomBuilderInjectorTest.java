package io.quarkus.cyclonedx.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class EmbeddedSbomBuilderInjectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPACT = "{"
            + "\"bomFormat\":\"CycloneDX\",\"specVersion\":\"1.6\",\"version\":1,"
            + "\"metadata\":{"
            + "\"timestamp\":\"2026-01-01T00:00:00Z\","
            + "\"component\":{\"type\":\"application\",\"name\":\"acme\",\"version\":\"1.0\"},"
            + "\"tools\":{\"components\":["
            + "{\"type\":\"library\",\"group\":\"io.quarkus\",\"name\":\"quarkus-cyclonedx-generator\",\"version\":\"999\"}"
            + "]}"
            + "},"
            + "\"components\":["
            + "{\"type\":\"library\",\"group\":\"io.quarkus\",\"name\":\"quarkus-rest\",\"version\":\"1.0\","
            + "\"purl\":\"pkg:maven/io.quarkus/quarkus-rest@1.0\"}"
            + "],"
            + "\"dependencies\":[{\"ref\":\"acme\"}]"
            + "}";

    @Test
    void injectsIntoBothLocationsForCompactJson() throws Exception {
        String result = EmbeddedSbomBuilderInjector.injectIntoJson(COMPACT, "Mandrel", "23.1.2");

        JsonNode root = MAPPER.readTree(result);

        // top-level components: original component preserved, builder added
        JsonNode components = root.get("components");
        assertThat(components).isNotNull();
        assertThat(names(components)).contains("quarkus-rest", "Mandrel");

        JsonNode builder = findByName(components, "Mandrel");
        assertThat(builder.get("type").asText()).isEqualTo("library");
        assertThat(builder.get("group").asText()).isEqualTo("org.graalvm");
        assertThat(builder.get("version").asText()).isEqualTo("23.1.2");
        assertThat(builder.get("purl").asText()).isEqualTo("pkg:generic/Mandrel@23.1.2");
        assertThat(builder.get("bom-ref").asText()).isEqualTo("pkg:generic/Mandrel@23.1.2");

        // metadata.tools.components: generator preserved, builder added
        JsonNode toolComponents = root.get("metadata").get("tools").get("components");
        assertThat(names(toolComponents)).contains("quarkus-cyclonedx-generator", "Mandrel");
    }

    @Test
    void injectsIntoBothLocationsForPrettyJson() throws Exception {
        String pretty = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(MAPPER.readTree(COMPACT));

        String result = EmbeddedSbomBuilderInjector.injectIntoJson(pretty, "Oracle-GraalVM", "25.0.0");

        JsonNode root = MAPPER.readTree(result);
        assertThat(names(root.get("components"))).contains("quarkus-rest", "Oracle-GraalVM");
        assertThat(names(root.get("metadata").get("tools").get("components")))
                .contains("quarkus-cyclonedx-generator", "Oracle-GraalVM");
        assertThat(findByName(root.get("components"), "Oracle-GraalVM").get("purl").asText())
                .isEqualTo("pkg:generic/Oracle-GraalVM@25.0.0");
    }

    @Test
    void handlesEmptyTopLevelComponentsArray() throws Exception {
        String json = "{\"specVersion\":\"1.6\",\"metadata\":{\"tools\":{\"components\":[]}},\"components\":[]}";

        String result = EmbeddedSbomBuilderInjector.injectIntoJson(json, "GraalVM-CE", "25.0.0");

        JsonNode root = MAPPER.readTree(result);
        assertThat(names(root.get("components"))).containsExactly("GraalVM-CE");
        assertThat(names(root.get("metadata").get("tools").get("components"))).containsExactly("GraalVM-CE");
    }

    @Test
    void createsTopLevelComponentsWhenMissing() throws Exception {
        String json = "{\"specVersion\":\"1.6\",\"metadata\":{\"component\":{\"name\":\"acme\"}}}";

        String result = EmbeddedSbomBuilderInjector.injectIntoJson(json, "Mandrel", "23.1.2");

        JsonNode root = MAPPER.readTree(result);
        assertThat(root.get("components")).isNotNull();
        assertThat(names(root.get("components"))).containsExactly("Mandrel");
    }

    @Test
    void createsValidComponentsForEmptyRootObject() throws Exception {
        // Defensive: even a degenerate "{}" (never produced by the CycloneDX generator) must
        // yield valid JSON — no leading comma after the opening brace.
        String result = EmbeddedSbomBuilderInjector.injectIntoJson("{}", "Mandrel", "23.1.2");

        JsonNode root = MAPPER.readTree(result);
        assertThat(names(root.get("components"))).containsExactly("Mandrel");
    }

    @Test
    void skipsToolsComponentsForOldSchemaWhereToolsIsArray() throws Exception {
        // CycloneDX < 1.5 renders "tools" as an array of tool objects, not a ToolInformation object.
        String json = "{\"specVersion\":\"1.4\",\"metadata\":{"
                + "\"tools\":[{\"vendor\":\"io.quarkus\",\"name\":\"quarkus-cyclonedx-generator\",\"version\":\"999\"}]},"
                + "\"components\":[{\"type\":\"library\",\"name\":\"quarkus-rest\"}]}";

        String result = EmbeddedSbomBuilderInjector.injectIntoJson(json, "Mandrel", "23.1.2");

        JsonNode root = MAPPER.readTree(result);
        // top-level components still gets the builder
        assertThat(names(root.get("components"))).contains("quarkus-rest", "Mandrel");
        // the tools array is untouched (still contains only the generator)
        JsonNode tools = root.get("metadata").get("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).get("name").asText()).isEqualTo("quarkus-cyclonedx-generator");
    }

    @Test
    void isNotFooledByComponentsKeyInsideStringValues() throws Exception {
        // A decoy "components":[ sequence inside a string value must not be treated as an array.
        String json = "{\"specVersion\":\"1.6\","
                + "\"metadata\":{\"component\":{\"name\":\"acme\","
                + "\"description\":\"contains a \\\"components\\\":[ decoy and a } brace\"}},"
                + "\"components\":[{\"type\":\"library\",\"name\":\"real\"}]}";

        String result = EmbeddedSbomBuilderInjector.injectIntoJson(json, "Mandrel", "23.1.2");

        JsonNode root = MAPPER.readTree(result);
        assertThat(names(root.get("components"))).containsExactlyInAnyOrder("real", "Mandrel");
        // the decoy string is preserved verbatim
        assertThat(root.get("metadata").get("component").get("description").asText())
                .isEqualTo("contains a \"components\":[ decoy and a } brace");
    }

    @Test
    void doesNotConfuseNestedComponentComponentsWithTopLevel() throws Exception {
        // A top-level component with its own nested "components" (bundled artifacts).
        String json = "{\"specVersion\":\"1.6\",\"components\":["
                + "{\"type\":\"library\",\"name\":\"outer\",\"components\":["
                + "{\"type\":\"library\",\"name\":\"bundled\"}]}]}";

        String result = EmbeddedSbomBuilderInjector.injectIntoJson(json, "Mandrel", "23.1.2");

        JsonNode root = MAPPER.readTree(result);
        // builder is added to the ROOT components array, not the nested one
        assertThat(names(root.get("components"))).containsExactlyInAnyOrder("outer", "Mandrel");
        JsonNode outer = findByName(root.get("components"), "outer");
        assertThat(names(outer.get("components"))).containsExactly("bundled");
    }

    @Test
    void percentEncodesUnsafePurlCharacters() throws Exception {
        String result = EmbeddedSbomBuilderInjector.injectIntoJson(COMPACT, "GraalVM CE", "25.0.0-dev+9");

        JsonNode builder = findByName(MAPPER.readTree(result).get("components"), "GraalVM CE");
        assertThat(builder.get("purl").asText()).isEqualTo("pkg:generic/GraalVM%20CE@25.0.0-dev%2B9");
        // the human-readable name and version are stored unencoded
        assertThat(builder.get("version").asText()).isEqualTo("25.0.0-dev+9");
    }

    @Test
    void doesNotDuplicateBuilderBomRef() throws Exception {
        byte[] resource = COMPACT.getBytes(StandardCharsets.UTF_8);
        byte[] out = EmbeddedSbomBuilderInjector.inject(resource, "Mandrel", "25.0.4.1");
        JsonNode root = MAPPER.readTree(out);

        assertThat(root.findValuesAsText("bom-ref"))
                .contains("pkg:generic/Mandrel@25.0.4.1")
                .doesNotHaveDuplicates();
        JsonNode builder = findByName(root.get("components"), "Mandrel");
        assertThat(builder.path("bom-ref").asText()).isEqualTo("pkg:generic/Mandrel@25.0.4.1");
        JsonNode tool = findByName(root.get("metadata").get("tools").get("components"), "Mandrel");
        assertThat(tool.has("bom-ref")).isFalse();
        assertThat(tool.path("purl").asText()).isEqualTo(builder.path("purl").asText());
        assertThat(tool.path("version").asText()).isEqualTo(builder.path("version").asText());
    }

    @Test
    void rejectsUnrecognizedJson() {
        String notAnObject = "[1,2,3]";
        assertThatThrownBy(() -> EmbeddedSbomBuilderInjector.injectIntoJson(notAnObject, "Mandrel", "23.1.2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Embedded SBOM must be a JSON object");
    }

    private static List<String> names(JsonNode array) {
        List<String> result = new ArrayList<>();
        array.forEach(n -> result.add(n.get("name").asText()));
        return result;
    }

    private static JsonNode findByName(JsonNode array, String name) {
        for (JsonNode n : array) {
            if (name.equals(n.path("name").asText())) {
                return n;
            }
        }
        throw new AssertionError("No component named '" + name + "' in " + array);
    }
}
