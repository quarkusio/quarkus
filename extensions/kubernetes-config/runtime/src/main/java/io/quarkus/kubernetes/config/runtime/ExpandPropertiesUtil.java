package io.quarkus.kubernetes.config.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ExpandPropertiesUtil {

    static final String CONFIGMAPS = "configMaps";
    static final String SECRETS = "secrets";

    private static final String REFERENCE_NAME_SEPARATOR = "/";

    private ExpandPropertiesUtil() {

    }

    static String expandYaml(String type, String referenceName, String input) {
        StringBuilder sb = new StringBuilder();
        sb.append(input + System.lineSeparator());
        String[] parts = referenceName.split(Pattern.quote(REFERENCE_NAME_SEPARATOR));
        if (parts.length == 4) {
            String namespace = parts[0];
            String name = parts[1];

            if (isNotNull(namespace) && isNotNull(name)) {
                sb.append(join(type, namespace, name) + ":" + System.lineSeparator());
                input.lines().forEach(line -> sb.append(indent(line) + System.lineSeparator()));
            }

            if (isNotNull(name)) {
                sb.append(join(type, name) + ":" + System.lineSeparator());
                input.lines().forEach(line -> sb.append(indent(line) + System.lineSeparator()));
            }
        }

        return sb.toString();
    }

    static Map<String, String> expandMap(String type, String referenceName, Map<String, String> input) {
        // First, add the properties as they are. Example: prop=key.
        Map<String, String> expanded = new HashMap<>();
        expanded.putAll(input);

        // Next, append the prefixes according to the name: {namespace}/{name}/x/y.
        String[] parts = referenceName.split(Pattern.quote(REFERENCE_NAME_SEPARATOR));
        if (parts.length == 4) {
            String namespace = parts[0];
            String name = parts[1];
            if (isNotNull(namespace) && isNotNull(name)) {
                for (Map.Entry<String, String> entry : input.entrySet()) {
                    expanded.put(join(type, namespace, name) + "." + entry.getKey(), entry.getValue());
                }
            }

            if (isNotNull(name)) {
                for (Map.Entry<String, String> entry : input.entrySet()) {
                    expanded.put(join(type, name) + "." + entry.getKey(), entry.getValue());
                }
            }
        }
        return expanded;
    }

    private static String indent(String str) {
        return "  " + str;
    }

    private static String join(String... parts) {
        return Stream.of(parts).collect(Collectors.joining(REFERENCE_NAME_SEPARATOR));
    }

    private static boolean isNotNull(String str) {
        return str != null && !str.isEmpty() && !str.equalsIgnoreCase("null");
    }
}
