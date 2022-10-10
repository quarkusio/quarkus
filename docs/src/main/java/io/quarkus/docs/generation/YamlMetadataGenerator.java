package io.quarkus.docs.generation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Iterate over the documents in the source directory.
 * Creates two sets of files in the target directory:
 * <ul>
 * <li>{@code index*.yaml}, which contains metadata (id, title, file name, categories, summary, preamble)
 * from each document. One file is organized by document type, another is organized by file name.
 * <li>{@code errors*.yaml}, which lists all documents that have problems with required structure or
 * metadata. One file is organized by document type, another is organized by file name.
 * </ul>
 */
public class YamlMetadataGenerator {
    static Errors errors = new Errors();

    static Path docsDir() {
        Path path = Paths.get(System.getProperty("user.dir"));
        if (path.endsWith("docs")) {
            return path;
        }
        return path.resolve("docs");
    }

    enum Category {
        alt_languages("alt-languages", "Alternative languages"),
        architecture("architecture", "Architecture"),
        business_automation("business-automation", "Business Automation"),
        cloud("cloud", "Cloud"),
        command_line("command-line", "Command Line Applications"),
        compatibility("compatibility", "Compatibility"),
        contributing("contributing", "Contributing"),
        core("core", "Core"),
        data("data", "Data"),
        getting_started("getting-started", "Getting Started"),
        integration("integration", "Integration"),
        messaging("messaging", "Messaging"),
        miscellaneous("miscellaneous", "Miscellaneous"),
        observability("observability", "Observability"),
        reactive("reactive", "Reactive"),
        security("security", "Security"),
        serialization("serialization", "Serialization"),
        tooling("tooling", "Tooling"),
        web("web", "Web"),
        writing_extensions("writing-extensions", "Writing Extensions");

        final String id;
        final String name;

        Category(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public static void addAll(Set<Category> set, Object source, Path path) {
            if (source == null) {
                return;
            }
            for (String c : source.toString().split("\\s*,\\s*")) {
                try {
                    Category cat = Category.valueOf(c.toLowerCase().replace("-", "_"));
                    set.add(cat);
                } catch (IllegalArgumentException ex) {
                    errors.record("unknown-category", path, "Unknown category: " + c);
                }
            }
        }
    }

    enum Type {
        concepts("concepts", "Concepts"),
        howto("howto", "How-To Guides"),
        tutorial("tutorial", "Tutorial"),
        reference("reference", "Reference"),
        other("guide", "General Guides");

        final String name;
        final String id;

        Type(String id, String name) {
            this.name = name;
            this.id = id;
        }
    }

    public static void main(String[] args) throws Exception {
        Path rootDir = args.length >= 1
                ? Path.of(args[0])
                : docsDir().resolve("src/main/asciidoc");
        Path targetDir = args.length >= 2
                ? Path.of(args[1])
                : docsDir().resolve("target");

        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            throw new IllegalStateException(String.format("Source directory (%s) does not exist", rootDir.toAbsolutePath()));
        }
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            System.err.printf("Target directory (%s) does not exist. Exiting.%n", targetDir.toAbsolutePath());
            return;
        }

        Options options = Options.builder()
                .docType("book")
                .sourceDir(rootDir.toFile())
                .safe(SafeMode.UNSAFE)
                .build();
        Index index = new Index();

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            try (Stream<Path> pathStream = Files.list(rootDir)) {
                pathStream.filter(path -> !path.endsWith("attributes.adoc"))
                        .filter(path -> !path.getFileName().toString().startsWith("README"))
                        .filter(path -> path.getFileName().toString().endsWith(".adoc"))
                        .forEach(path -> {
                            Document doc = asciidoctor.loadFile(path.toFile(), options);
                            String title = doc.getDoctitle();
                            String id = doc.getId();
                            Object categories = doc.getAttribute("categories");
                            Object summary = doc.getAttribute("summary");

                            Optional<StructuralNode> preambleNode = doc.getBlocks().stream()
                                    .filter(b -> "preamble".equals(b.getNodeName()))
                                    .findFirst();

                            final String summaryString;
                            if (preambleNode.isPresent()) {
                                Optional<String> content = preambleNode.get().getBlocks().stream()
                                        .filter(b -> "paragraph".equals(b.getContext()))
                                        .map(b -> b.getContent().toString())
                                        .filter(s -> !s.contains("attributes.adoc"))
                                        .findFirst();

                                summaryString = getSummary(summary, content);

                                if (content.isPresent()) {
                                    index.add(new DocMetadata(title, path, summaryString, categories, id));
                                } else {
                                    errors.record("empty-preamble", path);
                                    index.add(new DocMetadata(title, path, summaryString, categories, id));
                                }
                            } else {
                                errors.record("missing-preamble", path);
                                summaryString = getSummary(summary, Optional.empty());
                                index.add(new DocMetadata(title, path, summaryString, categories, id));
                            }

                            long spaceCount = summaryString.chars().filter(c -> c == (int) ' ').count();
                            if (spaceCount > 26) {
                                errors.record("summary-too-long", path);
                            }
                        });
            }
        }

        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        Map<String, DocMetadata> metadata = index.metadataByFile();
        Map<String, Collection<String>> errorsByFile = errors.errorsByFile(metadata);

        om.writeValue(targetDir.resolve("indexByType.yaml").toFile(), index);
        om.writeValue(targetDir.resolve("indexByFile.yaml").toFile(), metadata);

        om.writeValue(targetDir.resolve("errorsByType.yaml").toFile(), errors);
        om.writeValue(targetDir.resolve("errorsByFile.yaml").toFile(), errorsByFile);
    }

    static String getSummary(Object summary, Optional<String> preamble) {
        String result = (summary != null ? summary.toString() : preamble.orElse(""))
                .trim()
                .replaceAll("\n", " ") // undo semantic line endings
                .replaceAll("\\s+", " ") // condense whitespace
                .replaceAll("<[^>]+>(.*?)</[^>]+>", "$1"); // strip html tags
        int pos = result.indexOf(". "); // Find the end of the first sentence.
        if (pos >= 1) {
            return result.substring(0, pos + 1).trim();
        }
        return result;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "errors")
    static class Errors {
        Map<String, Collection<String>> errors = new HashMap<>();
        Map<String, Collection<String>> messagesByFile = new HashMap<>();

        void record(String errorKey, Path path) {
            record(errorKey, path, null);
        }

        void record(String errorKey, Path path, String message) {
            errors.computeIfAbsent(errorKey, k -> new HashSet<>()).add(path.getFileName().toString());
            if (message == null) {
                message = getMessageforKey(errorKey);
            }
            messagesByFile.computeIfAbsent(path.toString(), k -> new ArrayList<>()).add(message);
        }

        private String getMessageforKey(String errorKey) {
            switch (errorKey) {
                case "empty-preamble":
                    return "Document preamble is empty.";
                case "missing-preamble":
                    return "Document does not have a preamble.";
                case "summary-too-long":
                    return "Document summary (either summary attribute or the preamble) is longer than 26 words.";
                case "missing-id":
                    return "Document does not define an id.";
                case "missing-categories":
                    return "Document does not specify associated categories";
                case "not-diataxis-type":
                    return "Document does not follow naming conventions (type not recognized).";
            }
            return errorKey;
        }

        public Map<String, Collection<String>> getErrors() {
            return errors;
        }

        Map<String, Collection<String>> errorsByFile(Map<String, DocMetadata> metadata) {
            return messagesByFile;
        }
    }

    static class Index {
        Map<Type, IndexByType> types = new HashMap<>();

        public Map<String, IndexByType> getTypes() {
            return types.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().id, Map.Entry::getValue));
        }

        public void add(DocMetadata doc) {
            types.computeIfAbsent(doc.type, IndexByType::new).add(doc);
        }

        public Map<String, DocMetadata> metadataByFile() {
            return types.values().stream()
                    .flatMap(v -> v.getIndex().values().stream())
                    .collect(Collectors.toMap(v -> v.filename, v -> v));
        }
    }

    static class IndexByType {
        String id;
        String name;
        Map<String, DocMetadata> docs = new TreeMap<>();

        IndexByType(Type c) {
            this.name = c.name;
            this.id = c.id;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Map<String, DocMetadata> getIndex() {
            return docs;
        }

        public void add(DocMetadata doc) {
            docs.put(doc.filename, doc);
        }
    }

    @JsonInclude(value = Include.NON_EMPTY)
    static class DocMetadata {
        String title;
        String filename;
        String summary;
        Set<Category> categories = new HashSet<>();
        String id;
        Type type;

        public DocMetadata(String title, Path path, String summary, Object categories, String id) {
            this.id = id;
            this.title = title;
            this.filename = path.getFileName().toString();
            this.summary = summary;
            Category.addAll(this.categories, categories, path);

            if (this.categories.contains(Category.getting_started)) {
                this.type = Type.tutorial;
            } else if (filename.endsWith("-concepts.adoc")) {
                this.type = Type.concepts;
            } else if (filename.endsWith("-howto.adoc")) {
                this.type = Type.howto;
            } else if (filename.endsWith("-tutorial.adoc")) {
                this.type = Type.tutorial;
            } else if (filename.endsWith("-reference.adoc")) {
                this.type = Type.reference;
            } else {
                this.type = Type.other;
                errors.record("not-diataxis-type", path);
            }

            if (id == null) {
                errors.record("missing-id", path);
            } else if (type != Type.other && !id.startsWith(type.id)) {
                errors.record("incorrect-id", path,
                        String.format("The document id (%s) does not start with the correct prefix, should start with '%s-'%n",
                                id, type.id));
            }

            if (this.categories.isEmpty()) {
                errors.record("missing-categories", path);
            }
        }

        public String getId() {
            return id;
        }

        public String getFilename() {
            return filename;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }

        public List<String> getCategories() {
            return categories.stream()
                    .map(x -> x.id)
                    .collect(Collectors.toList());
        }

        public String getType() {
            return type.id;
        }
    }
}
