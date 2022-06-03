package io.quarkus.docs.generation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
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
 * Create two files in the target directory:
 * - index.yaml, which contains metadata (id, title, filename, keywords, summary, preamble) from each document
 * - errors.yaml, which lists all documents that have problems with required structure or metadata
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

        Options options = Options.builder().build();
        Index index = new Index();

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            try (Stream<Path> pathStream = Files.list(rootDir)) {
                pathStream.filter(path -> !path.endsWith("attributes.adoc"))
                        .filter(path -> path.getFileName().toString().endsWith(".adoc"))
                        .forEach(path -> {
                            Document doc = asciidoctor.loadFile(path.toFile(), options);
                            String title = doc.getDoctitle();
                            String id = doc.getId();
                            Object keywords = doc.getAttribute("keywords");
                            Object summary = doc.getAttribute("summary");

                            Optional<StructuralNode> preambleNode = doc.getBlocks().stream()
                                    .filter(b -> "preamble".equals(b.getNodeName()))
                                    .findFirst();

                            if (preambleNode.isPresent()) {
                                Optional<String> content = preambleNode.get().getBlocks().stream()
                                        .filter(b -> "paragraph".equals(b.getContext()))
                                        .map(b -> b.getContent().toString())
                                        .filter(s -> !s.contains("attributes.adoc"))
                                        .findFirst();

                                if (content.isPresent()) {
                                    index.add(new DocMetadata(title, path, getSummary(summary, content), keywords, id));
                                } else {
                                    System.err.format("%s (%s) does not have text in the preamble%n", path, title);
                                    errors.record("empty-preamble", path);
                                    index.add(new DocMetadata(title, path, getSummary(summary, content), keywords, id));
                                }
                            } else {
                                System.err.format("[WARN] %s (%s) does not have a preamble section%n", path, title);
                                errors.record("missing-preamble", path);
                                index.add(new DocMetadata(title, path, getSummary(summary, Optional.empty()), keywords, id));
                            }
                        });
            }
        }

        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

        Map<String, DocMetadata> metadata = index.metadataByFile();
        om.writeValue(targetDir.resolve("indexByType.yaml").toFile(), index);
        om.writeValue(targetDir.resolve("indexByFile.yaml").toFile(), metadata);

        Map<String, List<String>> errorsByFile = errors.errorsByFile(metadata);
        om.writeValue(targetDir.resolve("errorsByType.yaml").toFile(), errors);
        om.writeValue(targetDir.resolve("errorsByFile.yaml").toFile(), errorsByFile);
    }

    static String getSummary(Object summary, Optional<String> content) {
        if (summary != null) {
            return summary.toString();
        }
        return content.orElse("");
    }

    enum Type {
        concepts("Concepts", "concepts"),
        howto("How-To Guides", "howto"),
        getstarted("Getting Started", "getting-started"),
        tutorial("Tutorial", "tutorial"),
        reference("Reference", "reference"),
        other("General Guides", "guide");

        final String name;
        final String id;

        Type(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "errors")
    static class Errors {
        Map<String, List<String>> errors = new HashMap<>();

        public Map<String, List<String>> getErrors() {
            return errors;
        }

        void record(String key, Path path) {
            errors.computeIfAbsent(key, k -> new ArrayList<>()).add(path.getFileName().toString());
        }

        Map<String, List<String>> errorsByFile(Map<String, DocMetadata> metadata) {
            Map<String, List<String>> errorsByFile = errors.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v -> new String[] { v, e.getKey() }))
                    .collect(Collectors.groupingBy(s -> s[0],
                            TreeMap::new,
                            Collectors.mapping(s -> s[1], Collectors.toList())));

            errorsByFile.entrySet().forEach(e -> {
                DocMetadata dm = metadata.get(e.getKey());
                if (dm.type == Type.other) {
                    e.getValue().add("not-diataxis-type");
                }
            });
            return errorsByFile;
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
        String description;
        String keywords;
        String id;

        Type type;

        public DocMetadata(String title, Path path, String description, Object keywords, String id) {
            this.id = id;
            this.title = title;
            this.filename = path.getFileName().toString();
            this.keywords = keywords == null ? null : keywords.toString();
            this.description = description
                    .replaceAll("\n", " ") // undo semantic line endings
                    .replaceAll("\\s+", " ")
                    .replaceAll("<[^>]+>(.*?)</[^>]+>", "$1"); // strip html tags

            if (filename.endsWith("-concepts.adoc")) {
                this.type = Type.concepts;
            } else if (filename.endsWith("-howto.adoc")) {
                this.type = Type.howto;
            } else if (filename.endsWith("-tutorial.adoc")) {
                this.type = Type.tutorial;
            } else if (filename.endsWith("-reference.adoc")) {
                this.type = Type.reference;
            } else {
                this.type = Type.other;
            }

            if (id == null) {
                errors.record("missing-id", path);
            } else if (type != Type.other && !id.startsWith(type.id)) {
                System.err.format("[ERROR] %s id (%s) does not start with the correct prefix, should start with '%s-'%n",
                        filename, id, type.id);
                errors.record("incorrect-id", path);
            }
            if (keywords == null) {
                errors.record("missing-keywords", path);
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

        public String getDescription() {
            return description;
        }

        public String getKeywords() {
            return keywords;
        }

        public String getType() {
            return type.id;
        }
    }
}
