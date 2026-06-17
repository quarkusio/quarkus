package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generates AsciiDoc reference tables for {@code META-INF/quarkus-extension.yaml} from the
 * canonical JSON schema bundled in {@code quarkus-devtools-common}.
 * <p>
 * The schema is the single source of truth; this generator keeps the extension metadata guide
 * aligned with validation rules enforced at extension packaging time.
 */
public class ExtensionMetadataSchemaDocGenerator {

    private static final String SCHEMA_ID = "https://quarkus.io/schemas/quarkus-extension-schema.json";
    private static final String SCHEMA_SOURCE_PATH = "independent-projects/tools/devtools-common/src/main/resources/META-INF/quarkus-extension-schema.json";
    private static final String DEFAULT_DOC_GROUP = "template";

    private final ObjectMapper mapper = new ObjectMapper();
    private PrintStream out = System.out;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: ExtensionMetadataSchemaDocGenerator <output-adoc> <schema-json>");
        }
        ExtensionMetadataSchemaDocGenerator generator = new ExtensionMetadataSchemaDocGenerator();
        generator.generate(Path.of(args[0]), Path.of(args[1]));
    }

    public void generate(Path outputFile, Path schemaFile) throws IOException {
        if (outputFile != null) {
            Files.createDirectories(outputFile.getParent());
            out = new PrintStream(Files.newOutputStream(outputFile));
        }
        final JsonNode schema = mapper.readTree(schemaFile.toFile());
        final Map<String, List<FieldRow>> rowsByGroup = collectRows(schema, "", DEFAULT_DOC_GROUP);

        out.println("// Generated from " + SCHEMA_SOURCE_PATH + ". Do not edit.");
        out.println("// Regenerate by building the documentation module.");
        out.println("[[quarkus-extension-schema-reference]]");
        out.println("=== Extension descriptor schema reference");
        out.println();
        out.println("The canonical definition of `META-INF/quarkus-extension.yaml` is the JSON Schema at");
        out.println("link:" + SCHEMA_ID + "[" + SCHEMA_ID + "].");
        out.println("The extension Maven and Gradle plugins validate the packaged descriptor against this schema.");
        out.println("The tables below are generated from that schema at documentation build time.");
        out.println();
        out.println("Extension authors may reference the schema in template files for IDE assistance:");
        out.println();
        out.println("[source,yaml,subs=attributes+]");
        out.println("----");
        out.println("$schema: \"" + SCHEMA_ID + "\"");
        out.println("name: \"My Extension\"");
        out.println("metadata:");
        out.println("  status: \"stable\"");
        out.println("----");
        out.println();

        printGroup("Author template fields", "template", rowsByGroup);
        printGroup("Build-augmented fields", "build", rowsByGroup);
        printGroup("Extension offering fields", "offering", rowsByGroup);
    }

    private Map<String, List<FieldRow>> collectRows(JsonNode node, String pathPrefix, String inheritedGroup) {
        final Map<String, List<FieldRow>> rowsByGroup = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return rowsByGroup;
        }

        collectPropertyRows(node.get("properties"), pathPrefix, rowsByGroup, inheritedGroup);
        collectPatternPropertyRows(node.get("patternProperties"), pathPrefix, rowsByGroup, inheritedGroup);
        return rowsByGroup;
    }

    private void collectPropertyRows(JsonNode properties, String pathPrefix, Map<String, List<FieldRow>> rowsByGroup,
            String inheritedGroup) {
        if (properties == null || !properties.isObject()) {
            return;
        }
        final Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
        final List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        it.forEachRemaining(entries::add);
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        for (Map.Entry<String, JsonNode> entry : entries) {
            final String name = entry.getKey();
            final JsonNode property = entry.getValue();
            final String path = pathPrefix.isEmpty() ? name : pathPrefix + "." + name;
            final String group = docGroup(property, inheritedGroup);
            addRow(rowsByGroup, group, new FieldRow(path, describeType(property), description(property)));

            if ("object".equals(type(property))) {
                mergeRows(rowsByGroup, collectRows(property, path, group));
            }
        }
    }

    private void collectPatternPropertyRows(JsonNode patternProperties, String pathPrefix,
            Map<String, List<FieldRow>> rowsByGroup, String inheritedGroup) {
        if (patternProperties == null || !patternProperties.isObject()) {
            return;
        }
        final Iterator<Map.Entry<String, JsonNode>> it = patternProperties.fields();
        while (it.hasNext()) {
            final Map.Entry<String, JsonNode> entry = it.next();
            final String pattern = entry.getKey();
            final JsonNode property = entry.getValue();
            final String path = pathPrefix.isEmpty() ? pattern : pathPrefix + "." + pattern;
            final String group = docGroup(property, inheritedGroup);
            addRow(rowsByGroup, group, new FieldRow(path, describeType(property), description(property)));
        }
    }

    private static String docGroup(JsonNode property, String inheritedGroup) {
        final JsonNode value = property.get("x-doc-group");
        if (value != null && !value.asText().isBlank()) {
            return value.asText();
        }
        return inheritedGroup == null ? DEFAULT_DOC_GROUP : inheritedGroup;
    }

    private void mergeRows(Map<String, List<FieldRow>> target, Map<String, List<FieldRow>> source) {
        source.forEach((group, rows) -> rows.forEach(row -> addRow(target, group, row)));
    }

    private void addRow(Map<String, List<FieldRow>> rowsByGroup, String group, FieldRow row) {
        rowsByGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(row);
    }

    private void printGroup(String title, String group, Map<String, List<FieldRow>> rowsByGroup) {
        final List<FieldRow> rows = rowsByGroup.get(group);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        out.println("==== " + title);
        out.println();
        out.println("[cols=\"2,1,3\", options=\"header\"]");
        out.println("|===");
        out.println("| Field | Type | Description");
        for (FieldRow row : rows) {
            out.println("| `" + row.path + "`");
            out.println("| " + row.type);
            out.println("| " + escapeTableCell(row.description));
        }
        out.println("|===");
        out.println();
    }

    private static String escapeTableCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("|", "\\|");
    }

    private static String description(JsonNode property) {
        final JsonNode description = property.get("description");
        return description == null ? "" : description.asText();
    }

    private static String type(JsonNode property) {
        final JsonNode type = property.get("type");
        return type == null ? null : type.asText();
    }

    private static String describeType(JsonNode property) {
        final String primaryType = type(property);
        if (primaryType == null) {
            return "object";
        }
        if ("array".equals(primaryType)) {
            final JsonNode items = property.get("items");
            if (items != null && items.has("type")) {
                return "array of " + items.get("type").asText();
            }
            return "array";
        }
        if ("string".equals(primaryType) && property.has("enum")) {
            final StringBuilder sb = new StringBuilder("enum: ");
            final Iterator<JsonNode> it = property.get("enum").elements();
            while (it.hasNext()) {
                if (sb.length() > "enum: ".length()) {
                    sb.append(", ");
                }
                sb.append('`').append(it.next().asText()).append('`');
            }
            return sb.toString();
        }
        return primaryType;
    }

    private record FieldRow(String path, String type, String description) {
    }
}
