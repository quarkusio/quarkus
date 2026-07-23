package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Inject the hierarchical category path from categories.yaml into each guide's :categories-path: attribute.
 * <p>
 * Runs on the copied .adoc files in target/asciidoc/sources/ so source files are not modified.
 */
public class InjectCategories {

    public static void main(String[] args) throws Exception {
        Path targetDir = Path.of(args[0]);
        Path categoriesFile = Path.of(args[1]);

        System.out.println("[INFO] Injecting categories from: " + categoriesFile);

        Map<String, List<String>> guideToPaths = buildGuideToPaths(categoriesFile);

        int count = 0;
        try (Stream<Path> files = Files.list(targetDir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".adoc"))::iterator) {
                String filename = file.getFileName().toString();
                List<String> paths = guideToPaths.get(filename);
                if (paths == null || paths.isEmpty()) {
                    continue;
                }
                String categoriesPathValue = String.join(",", paths);
                injectAttribute(file, categoriesPathValue);
                count++;
            }
        }

        System.out.println("[INFO] Injected category paths into " + count + " guides");
    }

    @SuppressWarnings("unchecked")
    static Map<String, List<String>> buildGuideToPaths(Path categoriesFile) throws IOException {
        YAMLMapper om = new YAMLMapper();

        Map<String, Object> root;
        try (InputStream is = Files.newInputStream(categoriesFile)) {
            root = om.readValue(is, Map.class);
        }

        Map<String, List<String>> guideToPaths = new HashMap<>();
        List<Map<String, Object>> categories = (List<Map<String, Object>>) root.get("categories");
        if (categories != null) {
            for (Map<String, Object> category : categories) {
                String id = (String) category.get("id");
                collectPaths(category, id, guideToPaths);
            }
        }
        return guideToPaths;
    }

    @SuppressWarnings("unchecked")
    private static void collectPaths(Map<String, Object> node, String currentPath,
            Map<String, List<String>> guideToPaths) {
        List<String> guides = (List<String>) node.get("guides");
        if (guides != null) {
            for (String guide : guides) {
                guideToPaths.computeIfAbsent(guide, k -> new ArrayList<>()).add(currentPath);
            }
        }

        List<Map<String, Object>> subcategories = (List<Map<String, Object>>) node.get("subcategories");
        if (subcategories != null) {
            for (Map<String, Object> sub : subcategories) {
                String subId = (String) sub.get("id");
                collectPaths(sub, currentPath + "." + subId, guideToPaths);
            }
        }
    }

    static void injectAttribute(Path file, String categoriesPathValue) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<String> result = new ArrayList<>(lines);
        int headerEnd = findHeaderEnd(result);

        if (headerEnd == -1) {
            System.out.println("[WARN] Unable to find document header in: " + file);
            return;
        }

        if (!replaceHeaderAttribute(result, headerEnd, ":categories-path:", categoriesPathValue)) {
            int categoriesIndex = findHeaderAttribute(result, headerEnd, ":categories:");
            int insertionIndex = categoriesIndex != -1
                    ? categoriesIndex + 1
                    : findAttributeInsertionIndex(result, headerEnd);
            result.add(insertionIndex, ":categories-path: " + categoriesPathValue);
        }
        Files.write(file, result);
    }

    private static int findHeaderEnd(List<String> lines) {
        int titleIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("= ")) {
                titleIndex = i;
                break;
            }
        }
        if (titleIndex == -1) {
            return -1;
        }

        for (int i = titleIndex + 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                return i;
            }
        }
        return lines.size();
    }

    private static boolean replaceHeaderAttribute(List<String> lines, int headerEnd, String attribute, String value) {
        int attributeIndex = findHeaderAttribute(lines, headerEnd, attribute);
        if (attributeIndex == -1) {
            return false;
        }

        lines.set(attributeIndex, attribute + " " + value);
        return true;
    }

    private static int findHeaderAttribute(List<String> lines, int headerEnd, String attribute) {
        for (int i = 0; i < headerEnd; i++) {
            if (lines.get(i).startsWith(attribute)) {
                return i;
            }
        }
        return -1;
    }

    private static int findAttributeInsertionIndex(List<String> lines, int headerEnd) {
        for (int i = 0; i < headerEnd; i++) {
            if (lines.get(i).startsWith("include::_attributes.adoc[]")) {
                return i + 1;
            }
        }
        for (int i = 0; i < headerEnd; i++) {
            if (lines.get(i).startsWith("= ")) {
                return i + 1;
            }
        }
        return headerEnd;
    }
}
