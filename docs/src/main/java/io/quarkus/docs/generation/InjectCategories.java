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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Inject the hierarchical category path from categories.yaml into each guide's :categories: attribute.
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
                String categoriesValue = String.join(",", paths);
                injectAttribute(file, categoriesValue);
                count++;
            }
        }

        System.out.println("[INFO] Injected categories into " + count + " guides");
    }

    @SuppressWarnings("unchecked")
    static Map<String, List<String>> buildGuideToPaths(Path categoriesFile) throws IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

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

    static void injectAttribute(Path file, String categoriesValue) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<String> result = new ArrayList<>(lines.size());
        boolean replaced = false;

        for (String line : lines) {
            if (line.startsWith(":categories:")) {
                result.add(":categories: " + categoriesValue);
                replaced = true;
            } else {
                result.add(line);
            }
        }

        if (!replaced) {
            // Insert after the include::_attributes.adoc[] line if present, otherwise after the title
            List<String> output = new ArrayList<>(lines.size() + 1);
            boolean inserted = false;
            for (String line : lines) {
                output.add(line);
                if (!inserted && line.startsWith("include::_attributes.adoc[]")) {
                    output.add(":categories: " + categoriesValue);
                    inserted = true;
                }
            }
            if (!inserted) {
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("= ")) {
                        output = new ArrayList<>(lines.size() + 1);
                        output.addAll(lines.subList(0, i + 1));
                        output.add(":categories: " + categoriesValue);
                        output.addAll(lines.subList(i + 1, lines.size()));
                        break;
                    }
                }
            }
            result = output;
        }

        Files.write(file, result);
    }
}
