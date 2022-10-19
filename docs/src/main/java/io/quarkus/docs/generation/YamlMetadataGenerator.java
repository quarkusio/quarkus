package io.quarkus.docs.generation;

import java.io.IOException;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;
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
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Iterate over the documents in the source directory.
 * Creates two sets of files in the target directory:
 * <ul>
 * <li>{@code index*.yaml}, which contains metadata (id, title, file name,
 * categories, summary, preamble)
 * from each document. One file is organized by document type, another is
 * organized by file name.
 * <li>{@code errors*.yaml}, which lists all documents that have problems with
 * required structure or
 * metadata. One file is organized by document type, another is organized by
 * file name.
 * </ul>
 */
public class YamlMetadataGenerator {
    private static Errors errors = new Errors();

    public static void main(String[] args) throws Exception {
        System.out.println("[INFO] Creating YAML metadata generator: " + List.of(args));
        YamlMetadataGenerator generator = new YamlMetadataGenerator()
                .setSrcDir(args.length >= 1
                        ? Path.of(args[0])
                        : docsDir().resolve("src/main/asciidoc"))
                .setTargetDir(args.length >= 2
                        ? Path.of(args[1])
                        : docsDir().resolve("target"));

        System.out.println("[INFO] Generating metadata index");
        generator.generateIndex();
        System.out.println("[INFO] Writing metadata index and error files");
        generator.writeYamlFiles();
        System.out.println("[INFO] Done");
    }

    Path srcDir;
    Path targetDir;
    final Index index = new Index();
    Predicate<String> filePatternFilter;

    public YamlMetadataGenerator setSrcDir(Path srcDir) {
        this.srcDir = srcDir;
        return this;
    }

    public YamlMetadataGenerator setTargetDir(Path targetDir) {
        this.targetDir = targetDir;
        return this;
    }

    public YamlMetadataGenerator setFileFilterPattern(String filter) {
        if (filter != null && !filter.isBlank()) {
            filePatternFilter = Pattern.compile(filter).asPredicate();
        }
        return this;
    }

    public YamlMetadataGenerator setFileList(final Collection<String> fileNames) {
        if (fileNames != null && !fileNames.isEmpty()) {
            filePatternFilter = new Predicate<String>() {
                @Override
                public boolean test(String p) {
                    return fileNames.contains(p);
                }
            };
        }
        return this;
    }

    public void writeYamlFiles() throws StreamWriteException, DatabindException, IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        Map<String, DocMetadata> metadata = index.metadataByFile();

        om.writeValue(targetDir.resolve("indexByType.yaml").toFile(), index);
        om.writeValue(targetDir.resolve("indexByFile.yaml").toFile(), metadata);

        om.writeValue(targetDir.resolve("errorsByType.yaml").toFile(), errors);
        om.writeValue(targetDir.resolve("errorsByFile.yaml").toFile(), errors.messagesByFile);
    }

    public Index generateIndex() throws IOException {
        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist", srcDir.toAbsolutePath()));
        }
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            throw new IllegalStateException(
                    String.format("Target directory (%s) does not exist. Exiting.%n", targetDir.toAbsolutePath()));
        }
        errors.setRoot(srcDir);

        Options options = Options.builder()
                .docType("book")
                .sourceDir(srcDir.toFile())
                .safe(SafeMode.UNSAFE)
                .build();

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            try (Stream<Path> pathStream = Files.list(srcDir)) {
                pathStream.filter(path -> includeFile(path.getFileName().toString()))
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

        return index;
    }

    boolean includeFile(String fileName) {
        if (fileName.startsWith("attributes") || fileName.equals("README.adoc")) {
            return false;
        }
        if (fileName.endsWith(".adoc")) {
            if (filePatternFilter != null && !filePatternFilter.test(fileName)) {
                return false;
            }
            return true;
        }
        return false;
    }

    String getSummary(Object summary, Optional<String> preamble) {
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "errors")
    private static class Errors {
        String root;
        Map<String, Collection<String>> errors = new HashMap<>();
        Map<String, Collection<String>> messagesByFile = new HashMap<>();

        void setRoot(Path root) {
            this.root = root.toString();
            errors.clear();
            messagesByFile.clear();
        }

        void record(String errorKey, Path path) {
            record(errorKey, path, null);
        }

        void record(String errorKey, Path path, String message) {
            String filename = path.getFileName().toString().replace(root, "");
            errors.computeIfAbsent(errorKey, k -> new HashSet<>()).add(filename);
            if (message == null) {
                message = getMessageforKey(errorKey);
            }
            messagesByFile.computeIfAbsent(filename, k -> new ArrayList<>()).add(message);
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

        Map<String, Collection<String>> errorsByFile() {
            return messagesByFile;
        }
    }

    public static class Index {
        Map<Type, IndexByType> types = new HashMap<>();

        public Map<String, String> getCategories() {
            return Stream.of(Category.values())
                    .collect(Collectors.toMap(c -> c.id, c -> c.name));
        }

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

        // convenience
        public Map<String, Collection<String>> errorsByFile() {
            return errors.errorsByFile();
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

        DocMetadata(String title, Path path, String summary, Object categories, String id) {
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
                        String.format(
                                "The document id (%s) does not start with the correct prefix, should start with '%s-'%n",
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
