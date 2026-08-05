package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Inject guide categories from categories.yaml into the copied AsciiDoc sources.
 * <p>
 * Runs on the copied .adoc files in target/asciidoc/sources/ so source files are not modified.
 */
public class InjectCategories {

    public static void main(String[] args) throws Exception {
        Path targetDir = Path.of(args[0]);
        Path categoriesFile = Path.of(args[1]);

        System.out.println("[INFO] Injecting categories from: " + categoriesFile);

        Map<String, GuideCategories> guideToCategories = buildGuideToCategories(categoriesFile);

        int count = 0;
        try (Stream<Path> files = Files.list(targetDir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".adoc"))::iterator) {
                String filename = file.getFileName().toString();
                GuideCategories categories = guideToCategories.get(filename);
                if (categories == null || categories.isEmpty()) {
                    continue;
                }
                injectAttributes(file, categories.categoriesValue(), categories.pathsValue());
                count++;
            }
        }

        System.out.println("[INFO] Injected categories into " + count + " guides");
    }

    @SuppressWarnings("unchecked")
    static Map<String, GuideCategories> buildGuideToCategories(Path categoriesFile) throws IOException {
        YAMLMapper om = new YAMLMapper();

        Map<String, Object> root;
        try (InputStream is = Files.newInputStream(categoriesFile)) {
            root = om.readValue(is, Map.class);
        }

        Map<String, GuideCategories> guideToCategories = new HashMap<>();
        List<Map<String, Object>> categories = (List<Map<String, Object>>) root.get("categories");
        if (categories != null) {
            for (Map<String, Object> category : categories) {
                String id = (String) category.get("id");
                collectCategories(category, id, id, guideToCategories);
            }
        }
        return guideToCategories;
    }

    @SuppressWarnings("unchecked")
    private static void collectCategories(Map<String, Object> node, String category, String currentPath,
            Map<String, GuideCategories> guideToCategories) {
        List<String> guides = (List<String>) node.get("guides");
        if (guides != null) {
            for (String guide : guides) {
                guideToCategories.computeIfAbsent(guide, k -> new GuideCategories()).add(category, currentPath);
            }
        }

        List<Map<String, Object>> subcategories = (List<Map<String, Object>>) node.get("subcategories");
        if (subcategories != null) {
            for (Map<String, Object> sub : subcategories) {
                String subId = (String) sub.get("id");
                collectCategories(sub, category, currentPath + "." + subId, guideToCategories);
            }
        }
    }

    static void injectAttributes(Path file, String categoriesValue, String categoriesPathValue) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<String> result = new ArrayList<>(lines);
        int headerEnd = findHeaderEnd(result);

        if (headerEnd == -1) {
            System.out.println("[WARN] Unable to find document header in: " + file);
            return;
        }

        int categoriesIndex = replaceOrInsertHeaderAttribute(result, headerEnd,
                ":categories:", categoriesValue, findAttributeInsertionIndex(result, headerEnd));
        headerEnd = findHeaderEnd(result);
        if (!replaceHeaderAttribute(result, headerEnd, ":categories-path:", categoriesPathValue)) {
            result.add(categoriesIndex + 1, ":categories-path: " + categoriesPathValue);
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

    private static int replaceOrInsertHeaderAttribute(List<String> lines, int headerEnd, String attribute, String value,
            int insertionIndex) {
        int attributeIndex = findHeaderAttribute(lines, headerEnd, attribute);
        if (attributeIndex != -1) {
            lines.set(attributeIndex, attribute + " " + value);
            return attributeIndex;
        }

        lines.add(insertionIndex, attribute + " " + value);
        return insertionIndex;
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

    static final class GuideCategories {
        private final Set<String> categories = new LinkedHashSet<>();
        private final Set<String> paths = new LinkedHashSet<>();

        private void add(String category, String path) {
            categories.add(category);
            paths.add(path);
        }

        boolean isEmpty() {
            return categories.isEmpty() && paths.isEmpty();
        }

        String categoriesValue() {
            return String.join(",", categories);
        }

        String pathsValue() {
            return String.join(",", paths);
        }
    }
}
