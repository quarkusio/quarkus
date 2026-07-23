package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Check that all guides are referenced in at least one category in categories.yaml.
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

        Set<String> categorizedGuides = extractGuidesFromCategories(categoriesFile);
        Set<String> allGuides = listGuides(srcDir);

        Set<String> missingGuides = new TreeSet<>(allGuides);
        missingGuides.removeAll(categorizedGuides);

        if (!missingGuides.isEmpty()) {
            StringBuilder errorLog = new StringBuilder(
                    "The following guides are not referenced in any category in categories.yaml:\n\n");
            for (String guide : missingGuides) {
                errorLog.append("- ").append(guide).append("\n");
            }
            errorLog.append("\nPlease add them to the appropriate category in src/main/resources/categories.yaml");
            throw new IllegalStateException(errorLog.toString());
        }

        System.out.println("[INFO] All guides are properly categorized");
    }

    @SuppressWarnings("unchecked")
    static Set<String> extractGuidesFromCategories(Path categoriesFile) throws IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

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
