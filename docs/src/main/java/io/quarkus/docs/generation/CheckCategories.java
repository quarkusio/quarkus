package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Check that categories.yaml references existing guides and uses category IDs known to the YAML metadata generator.
 */
public class CheckCategories {

    private static final Set<String> IGNORED_GUIDES = Set.of(
            "_attributes.adoc",
            "_attributes-local.adoc",
            "README.adoc");

    public static void main(String[] args) throws Exception {
        Path srcDir = args.length >= 1
                ? Path.of(args[0])
                : docsDir().resolve("src/main/asciidoc");
        Path categoriesFile = args.length >= 2
                ? Path.of(args[1])
                : docsDir().resolve("src/main/resources/categories.yaml");

        System.out.println("[INFO] Checking categories using: " + categoriesFile);

        Set<String> categoryIds = extractCategoryIdsFromCategories(categoriesFile);
        Set<String> categorizedGuides = extractGuidesFromCategories(categoriesFile);
        Set<String> allGuides = listGuides(srcDir);

        Set<String> unknownCategoryIds = new TreeSet<>(categoryIds);
        unknownCategoryIds.removeAll(knownYamlMetadataCategoryIds());

        Set<String> missingGuides = new TreeSet<>(allGuides);
        missingGuides.removeAll(categorizedGuides);

        Set<String> staleGuides = new TreeSet<>(categorizedGuides);
        staleGuides.removeAll(allGuides);

        StringBuilder errorLog = new StringBuilder();
        if (!unknownCategoryIds.isEmpty()) {
            appendEntries(errorLog,
                    "The following top-level categories in categories.yaml are not recognized by YamlMetadataGenerator:",
                    unknownCategoryIds);
            errorLog.append(
                    "\nPlease add them to YamlMetadataGenerator.Category or use an existing top-level category.\n");
        }
        if (!missingGuides.isEmpty()) {
            appendEntries(errorLog,
                    "The following guides are not referenced in any category in categories.yaml:",
                    missingGuides);
            errorLog.append("\nPlease add them to the appropriate category in src/main/resources/categories.yaml\n");
        }
        if (!staleGuides.isEmpty()) {
            appendEntries(errorLog,
                    "The following guides are referenced in categories.yaml but do not exist:",
                    staleGuides);
            errorLog.append("\nPlease remove or update these entries in src/main/resources/categories.yaml\n");
        }

        if (errorLog.length() > 0) {
            throw new IllegalStateException(errorLog.toString());
        }

        System.out.println("[INFO] All guides are properly categorized");
    }

    private static void appendEntries(StringBuilder errorLog, String message, Set<String> entries) {
        if (errorLog.length() > 0) {
            errorLog.append("\n");
        }
        errorLog.append(message).append("\n\n");
        for (String entry : entries) {
            errorLog.append("- ").append(entry).append("\n");
        }
    }

    static Set<String> knownYamlMetadataCategoryIds() {
        return Arrays.stream(YamlMetadataGenerator.Category.values())
                .map(category -> category.id)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    static Set<String> extractCategoryIdsFromCategories(Path categoriesFile) throws IOException {
        YAMLMapper om = new YAMLMapper();

        Map<String, Object> root;
        try (InputStream is = Files.newInputStream(categoriesFile)) {
            root = om.readValue(is, Map.class);
        }

        Set<String> categoryIds = new TreeSet<>();
        List<Map<String, Object>> categories = (List<Map<String, Object>>) root.get("categories");
        if (categories != null) {
            for (Map<String, Object> category : categories) {
                categoryIds.add((String) category.get("id"));
            }
        }
        return categoryIds;
    }

    @SuppressWarnings("unchecked")
    static Set<String> extractGuidesFromCategories(Path categoriesFile) throws IOException {
        YAMLMapper om = new YAMLMapper();

        Map<String, Object> root;
        try (InputStream is = Files.newInputStream(categoriesFile)) {
            root = om.readValue(is, Map.class);
        }

        Set<String> guides = new HashSet<>();
        List<Map<String, Object>> categories = (List<Map<String, Object>>) root.get("categories");
        if (categories != null) {
            for (Map<String, Object> category : categories) {
                collectGuides(category, guides);
            }
        }
        return guides;
    }

    @SuppressWarnings("unchecked")
    private static void collectGuides(Map<String, Object> node, Set<String> guides) {
        List<String> guideList = (List<String>) node.get("guides");
        if (guideList != null) {
            guides.addAll(guideList);
        }
        List<Map<String, Object>> subcategories = (List<Map<String, Object>>) node.get("subcategories");
        if (subcategories != null) {
            for (Map<String, Object> subcategory : subcategories) {
                collectGuides(subcategory, guides);
            }
        }
    }

    static Set<String> listGuides(Path srcDir) throws IOException {
        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist", srcDir.toAbsolutePath()));
        }

        Set<String> guides = new TreeSet<>();
        try (Stream<Path> pathStream = Files.list(srcDir)) {
            pathStream
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.endsWith(".adoc")
                                && !fileName.startsWith("_attributes")
                                && !IGNORED_GUIDES.contains(fileName);
                    })
                    .forEach(path -> guides.add(path.getFileName().toString()));
        }
        return guides;
    }

    private static Path docsDir() {
        Path path = Paths.get(System.getProperty("user.dir"));
        if (path.endsWith("docs")) {
            return path;
        }
        return path.resolve("docs");
    }
}
