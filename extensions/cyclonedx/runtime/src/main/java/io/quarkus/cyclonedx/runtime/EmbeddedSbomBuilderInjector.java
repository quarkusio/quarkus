package io.quarkus.cyclonedx.runtime;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Injects a component describing the GraalVM/Mandrel native-image builder into the
 * embedded application SBOM.
 * <p>
 * This class is invoked at native-image <em>build</em> time from the generated
 * {@code SbomEmbedFeature} (see {@code SbomNativeImageFeatureStep}). It patches only the
 * copy of the SBOM that is embedded as the {@code sbom} global symbol; the classpath
 * resource served by the CycloneDX endpoint is left untouched.
 * <p>
 * The injected component reflects the actual native-image builder used to compile the
 * executable (distribution name and version, as reported by
 * {@code io.quarkus.runtime.graal.GraalVM.Version}). It is added to both the top-level
 * {@code components} array and, for CycloneDX 1.5+, {@code metadata.tools.components}.
 * <p>
 * This class intentionally takes the builder name and version as plain strings so that it
 * never links against {@code GraalVM} (which is deleted from the runtime image) and is
 * trivially unit-testable.
 */
public final class EmbeddedSbomBuilderInjector {

    private EmbeddedSbomBuilderInjector() {
    }

    /**
     * Injects the native-image builder component into the given SBOM.
     *
     * @param resourceBytes the SBOM resource bytes as stored on the classpath
     * @param builderName the native-image builder name, e.g. {@code "Mandrel"}
     * @param builderVersion the native-image builder version, e.g. {@code "23.1.2"}
     * @return the SBOM JSON with the builder component injected
     */
    public static byte[] inject(byte[] resourceBytes, String builderName, String builderVersion) {
        String json = new String(resourceBytes, StandardCharsets.UTF_8);
        String patched = injectIntoJson(json, builderName, builderVersion);
        return patched.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Injects the native-image builder component into the given JSON SBOM text.
     * <p>
     * Visible for testing.
     *
     * @param json the SBOM JSON (compact or pretty-printed)
     * @param builderName the native-image builder name
     * @param builderVersion the native-image builder version
     * @return the SBOM JSON with the builder component added to the top-level
     *         {@code components} array and to {@code metadata.tools.components} (when present)
     */
    static String injectIntoJson(String json, String builderName, String builderVersion) {
        String purl = "pkg:generic/" + percentEncode(builderName) + "@" + percentEncode(builderVersion);
        JsonObject component = createComponent(purl, builderName, builderVersion, true);
        JsonObject toolComponent = createComponent(purl, builderName, builderVersion, false);

        try (var reader = Json.createReader(new StringReader(json))) {
            JsonValue rootValue = reader.readValue();
            if (rootValue.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("Embedded SBOM must be a JSON object");
            }

            JsonObject root = rootValue.asJsonObject();
            JsonObjectBuilder rootBuilder = Json.createObjectBuilder(root);
            JsonArray components = root.getJsonArray("components");
            JsonArrayBuilder componentsBuilder = components == null ? Json.createArrayBuilder()
                    : Json.createArrayBuilder(components);
            componentsBuilder.add(0, component);
            rootBuilder.add("components", componentsBuilder);

            JsonObject metadata = root.getJsonObject("metadata");
            if (metadata != null) {
                JsonValue toolsValue = metadata.get("tools");
                if (toolsValue != null && toolsValue.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject tools = toolsValue.asJsonObject();
                    JsonArray toolComponents = tools.getJsonArray("components");
                    if (toolComponents != null) {
                        JsonObjectBuilder toolsBuilder = Json.createObjectBuilder(tools);
                        toolsBuilder.add("components", Json.createArrayBuilder(toolComponents).add(0, toolComponent));
                        rootBuilder.add("metadata", Json.createObjectBuilder(metadata).add("tools", toolsBuilder));
                    }
                }
            }

            StringWriter writer = new StringWriter(json.length());
            Json.createWriter(writer).write(rootBuilder.build());
            return writer.toString();
        }
    }

    private static JsonObject createComponent(String purl, String builderName, String builderVersion,
            boolean includeBomRef) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("type", "library")
                .add("group", "org.graalvm")
                .add("name", builderName)
                .add("version", builderVersion)
                .add("description", "GraalVM native-image builder used to build the native executable")
                .add("purl", purl);
        if (includeBomRef) {
            builder.add("bom-ref", purl);
        }
        return builder.build();
    }

    /**
     * Percent-encodes a PURL component, leaving the RFC 3986 unreserved set
     * ({@code A-Z a-z 0-9 . - _ ~}) unencoded. Matches {@code io.quarkus.sbom.Purl}, which
     * is not on the runtime classpath and therefore cannot be reused here.
     */
    static String percentEncode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = null;
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            boolean unreserved = (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9')
                    || b == '-' || b == '.' || b == '_' || b == '~';
            if (unreserved) {
                if (sb != null) {
                    sb.append((char) b);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(input.length() + 8);
                    sb.append(input, 0, i);
                }
                sb.append('%');
                sb.append(HEX[b >> 4]);
                sb.append(HEX[b & 0x0F]);
            }
        }
        return sb == null ? input : sb.toString();
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
}
