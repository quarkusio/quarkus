package io.quarkus.docs.generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
    private static Messages messages = new Messages();

    final static String INCL_ATTRIBUTES = "include::_attributes.adoc[]\n";
    final static String YAML_FRONTMATTER = "---\n";

    private static final String COMPATIBILITY_TOPIC = "compatibility";

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
        if ("true".equals(filter)) {
            filePatternFilter = x -> true;
        } else if ("false".equals(filter)) {
            filePatternFilter = x -> false;
        } else if (filter == null || filter.isBlank()) {
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
        ObjectMapper om = new ObjectMapper(
                new YAMLFactory()
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                        .disable(YAMLGenerator.Feature.SPLIT_LINES));
        Map<String, DocMetadata> metadata = index.metadataByFile();

        om.writeValue(targetDir.resolve("indexByType.yaml").toFile(), index);
        om.writeValue(targetDir.resolve("indexByFile.yaml").toFile(), metadata);

        om.writeValue(targetDir.resolve("relations.yaml").toFile(), index.relationsByUrl(metadata));

        om.writeValue(targetDir.resolve("errorsByType.yaml").toFile(), messages);
        om.writeValue(targetDir.resolve("errorsByFile.yaml").toFile(), messages.allByFile());
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
        messages.setRoot(srcDir);

        Options options = Options.builder()
                .docType("book")
                .sourceDir(srcDir.toFile())
                .baseDir(srcDir.toFile())
                .safe(SafeMode.UNSAFE)
                .build();

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            try (Stream<Path> pathStream = Files.list(srcDir)) {
                pathStream.filter(path -> includeFile(path.getFileName().toString()))
                        .forEach(path -> {
                            String str;
                            try {
                                str = Files.readString(path);
                            } catch (IOException e) {
                                messages.record("ioexception", path);
                                return;
                            }

                            // Strip off YAML frontmatter, if present
                            if (str.startsWith(YAML_FRONTMATTER)) {
                                int end = str.indexOf(YAML_FRONTMATTER, YAML_FRONTMATTER.length());
                                str = str.substring(end + YAML_FRONTMATTER.length());
                            }
                            Document doc = asciidoctor.load(str, options);

                            // Find the position of "include::_attributes.adoc[]"
                            // it should be part of the document header
                            int includeAttr = str.indexOf(INCL_ATTRIBUTES);
                            if (includeAttr < 0) {
                                messages.record("missing-attributes", path);
                            } else {
                                String prefix = str.substring(0, includeAttr);
                                if (prefix.contains("\n\n")) {
                                    messages.record("detached-attributes", path);
                                }
                            }

                            int titlePos = str.indexOf("\n= ");
                            int documentHeaderEnd = str.indexOf("\n\n", titlePos);
                            String documentHeader = str.substring(0, documentHeaderEnd);
                            if (documentHeader.contains(":toc:")) {
                                messages.record("toc", path);
                            }

                            String title = doc.getDoctitle();
                            String id = doc.getId();
                            Object categories = doc.getAttribute("categories");
                            Object keywords = doc.getAttribute("keywords");
                            Object summary = doc.getAttribute("summary");
                            Object type = doc.getAttribute("diataxis-type");
                            Object topics = doc.getAttribute("topics");
                            Object extensions = doc.getAttribute("extensions");

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
                                    index.add(new DocMetadata(title, path, summaryString, categories, keywords, topics,
                                            extensions, type, id));
                                } else {
                                    messages.record("empty-preamble", path);
                                    index.add(new DocMetadata(title, path, summaryString, categories, keywords, topics,
                                            extensions, type, id));
                                }
                            } else {
                                messages.record("missing-preamble", path);
                                summaryString = getSummary(summary, Optional.empty());
                                index.add(new DocMetadata(title, path, summaryString, categories, keywords, topics, extensions,
                                        type, id));
                            }

                            long spaceCount = summaryString.chars().filter(c -> c == (int) ' ').count();
                            if (spaceCount > 26) {
                                messages.record("summary-too-long", path);
                            }
                        });
            }
        }

        return index;
    }

    boolean includeFile(String fileName) {
        if (fileName.startsWith("_attributes") || fileName.equals("README.adoc")) {
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
        native_docs("native", "Native"),
        observability("observability", "Observability"),
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

        public Map<String, String> toMap() {
            Map<String, String> result = new HashMap<>();
            result.put("cat-id", id);
            result.put("category", name);
            return result;
        }

        public static void addAll(Set<Category> set, Object source, Path path) {
            if (source == null) {
                return;
            }
            for (String c : source.toString().split("\\s*,\\s*")) {
                String lower = c.toLowerCase();
                Optional<Category> match = Stream.of(Category.values())
                        .filter(cat -> cat.id.equals(lower))
                        .findFirst();
                if (match.isEmpty()) {
                    messages.record("unknown-category", path, "Unknown category: " + c);
                } else {
                    set.add(match.get());
                }
            }
        }
    }

    enum Type {
        concept("concepts", "Concept", "concept"),
        howto("howto", "How-To Guide"),
        tutorial("tutorial", "Tutorial"),
        reference("reference", "Reference"),
        other("guide", "General Guide");

        final String name;
        final String id;
        final String suffix;

        Type(String id, String name) {
            this(id, name, id);
        }

        Type(String id, String name, String suffix) {
            this.name = name;
            this.id = id;
            this.suffix = suffix;
        }

        public static Type fromObject(Object diataxisType) {
            if (diataxisType != null) {
                String type = diataxisType.toString().toLowerCase();
                for (Type value : Type.values()) {
                    if (value.id.equals(type) || value.suffix.equals(type)) {
                        return value;
                    }
                }
            }
            return null;
        }
    }

    private static class Messages {
        String root;
        Map<String, Collection<String>> errorsByFile = new TreeMap<>();
        Map<String, Collection<String>> warningsByFile = new TreeMap<>();
        public final Map<String, Collection<String>> errors = new TreeMap<>();

        void setRoot(Path root) {
            this.root = root.toString();
            errors.clear();
            errorsByFile.clear();
            warningsByFile.clear();
        }

        void record(String errorKey, Path path) {
            record(errorKey, path, null);
        }

        void record(String errorKey, Path path, String message) {
            String filename = path.getFileName().toString().replace(root, "");
            if (message == null) {
                message = getMessageforKey(errorKey);
            }

            if (isWarning(errorKey)) {
                warningsByFile.computeIfAbsent(filename, k -> new ArrayList<>()).add(message);
            } else {
                errorsByFile.computeIfAbsent(filename, k -> new ArrayList<>()).add(message);
            }
            errors.computeIfAbsent(errorKey, k -> new HashSet<>()).add(filename);
        }

        private boolean isWarning(String errorKey) {
            switch (errorKey) {
                case "missing-id":
                case "not-diataxis-type":
                    return true;
            }
            return false;
        }

        private String getMessageforKey(String errorKey) {
            switch (errorKey) {
                case "missing-attributes":
                    return "Document does not include common attributes: " + INCL_ATTRIBUTES;
                case "detached-attributes":
                    return "The document header ended (blank line) before common attributes were included.";
                case "empty-preamble":
                    return "Document preamble is empty. See https://quarkus.io/guides/doc-reference#abstracts-preamble";
                case "missing-preamble":
                    return "Document does not have a preamble. See https://quarkus.io/guides/doc-reference#abstracts-preamble";
                case "summary-too-long":
                    return "Document summary (either summary attribute or the preamble) is longer than 26 words. See https://quarkus.io/guides/doc-reference#doc-header-optional";
                case "missing-id":
                    return "Document does not define an id. See https://quarkus.io/guides/doc-reference#document-header";
                case "missing-categories":
                    return "Document does not specify associated categories. See https://quarkus.io/guides/doc-reference#categories";
                case "not-diataxis-type":
                    return "Document type not recognized. It either does not have a diataxis-type attribute or does not follow naming conventions. See https://quarkus.io/guides/doc-reference#document-header";
                case "toc":
                    return "A :toc: attribute is present in the document header (remove it)";
            }
            return errorKey;
        }

        public Map<String, FileMessages> allByFile() {
            Map<String, FileMessages> result = new TreeMap<>();
            errorsByFile.forEach((k, v) -> {
                FileMessages mr = result.computeIfAbsent(k, x -> new FileMessages());
                mr.errors = v;
            });
            warningsByFile.forEach((k, v) -> {
                FileMessages mr = result.computeIfAbsent(k, x -> new FileMessages());
                mr.warnings = v;
            });

            return result;
        }
    }

    public static class Index {
        Map<Type, IndexByType> types = new HashMap<>();

        public List<Map<String, String>> getCategories() {
            return Stream.of(Category.values())
                    .map(c -> c.toMap())
                    .collect(Collectors.toList());
        }

        public Map<String, Collection<DocMetadata>> getTypes() {
            return types.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().id, e -> e.getValue().getIndex()));
        }

        public void add(DocMetadata doc) {
            types.computeIfAbsent(doc.type, IndexByType::new).add(doc);
        }

        public Map<String, DocMetadata> metadataByFile() {
            return types.values().stream()
                    .flatMap(v -> v.getIndex().stream())
                    .collect(Collectors.toMap(v -> v.filename, v -> v, (o1, o2) -> o1, TreeMap::new));
        }

        public Map<String, DocRelations> relationsByUrl(Map<String, DocMetadata> metadataByFile) {
            Map<String, DocRelations> relationsByUrl = new TreeMap<>();

            for (Entry<String, DocMetadata> currentMetadataEntry : metadataByFile.entrySet()) {
                DocRelations docRelations = new DocRelations();

                for (Entry<String, DocMetadata> candidateMetadataEntry : metadataByFile.entrySet()) {
                    if (candidateMetadataEntry.getKey().equals(currentMetadataEntry.getKey())) {
                        continue;
                    }

                    DocMetadata candidateMetadata = candidateMetadataEntry.getValue();
                    int extensionMatches = 0;
                    for (String extension : currentMetadataEntry.getValue().getExtensions()) {
                        if (candidateMetadata.getExtensions().contains(extension)) {
                            extensionMatches++;
                        }
                    }
                    if (extensionMatches > 0) {
                        docRelations.sameExtensions.add(
                                new DocRelation(candidateMetadata.getTitle(), candidateMetadata.getUrl(),
                                        candidateMetadata.getType(), extensionMatches));
                    }

                    int topicMatches = 0;
                    for (String topic : currentMetadataEntry.getValue().getTopics()) {
                        if (candidateMetadata.getTopics().contains(topic)) {
                            topicMatches++;
                        }
                    }
                    if (topicMatches > 0 && (!candidateMetadata.getTopics().contains(COMPATIBILITY_TOPIC)
                            || currentMetadataEntry.getValue().getTopics().contains(COMPATIBILITY_TOPIC))) {
                        docRelations.sameTopics
                                .add(new DocRelation(candidateMetadata.getTitle(), candidateMetadata.getUrl(),
                                        candidateMetadata.getType(), topicMatches));
                    }
                }

                if (!docRelations.isEmpty()) {
                    relationsByUrl.put(currentMetadataEntry.getValue().getUrl(), docRelations);
                }
            }

            return relationsByUrl;
        }

        // convenience
        public Map<String, FileMessages> messagesByFile() {
            return messages.allByFile();
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

        public Collection<DocMetadata> getIndex() {
            return docs.values().stream()
                    .sorted(DocMetadata::compareTo)
                    .collect(Collectors.toList());
        }

        public void add(DocMetadata doc) {
            docs.put(doc.filename, doc);
        }
    }

    @JsonInclude(value = Include.NON_EMPTY)
    static class DocMetadata implements Comparable<DocMetadata> {
        String title;
        String filename;
        String summary;
        Set<String> keywords = new LinkedHashSet<>();
        Set<Category> categories = new TreeSet<>();
        Set<String> topics = new LinkedHashSet<>();
        Set<String> extensions = new LinkedHashSet<>();
        String id;
        Type type;

        DocMetadata(String title, Path path, String summary, Object categories, Object keywords,
                Object topics, Object extensions, Object diataxisType, String id) {
            this.id = id;
            this.title = title == null ? "" : title;
            this.filename = path.getFileName().toString();
            this.summary = summary;
            this.keywords = toSet(keywords);
            this.topics = toSet(topics);
            this.extensions = toSet(extensions);

            Category.addAll(this.categories, categories, path);

            this.type = Type.fromObject(diataxisType);
            if (this.type == null) {
                if (this.categories.contains(Category.getting_started)) {
                    this.type = Type.tutorial;
                } else if (filename.endsWith("-concept.adoc")) {
                    this.type = Type.concept;
                } else if (filename.endsWith("-howto.adoc")) {
                    this.type = Type.howto;
                } else if (filename.endsWith("-tutorial.adoc")) {
                    this.type = Type.tutorial;
                } else if (filename.endsWith("-reference.adoc")) {
                    this.type = Type.reference;
                } else {
                    this.type = Type.other;
                    messages.record("not-diataxis-type", path);
                }
            }

            if (id == null) {
                messages.record("missing-id", path);
            }
            if (this.categories.isEmpty()) {
                messages.record("missing-categories", path);
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

        public String getUrl() {
            return "/guides/" + filename.replace(".adoc", "");
        }

        public String getCategories() {
            return categories.stream()
                    .map(x -> x.id)
                    .collect(Collectors.joining(", "));
        }

        public Set<String> toSet(Object value) {
            if (value == null) {
                return Set.of();
            }

            String valueString = value.toString().trim();
            if (valueString.isEmpty()) {
                return Set.of();
            }

            return Arrays.stream(valueString.split(","))
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        public String getKeywords() {
            return String.join(", ", keywords);
        }

        public Set<String> getExtensions() {
            return extensions;
        }

        public Set<String> getTopics() {
            return topics;
        }

        public String getType() {
            return type.id;
        }

        @Override
        public int compareTo(DocMetadata that) {
            return this.title.compareTo(that.title);
        }
    }

    @JsonInclude(value = Include.NON_EMPTY)
    public static class DocRelations {

        final Set<DocRelation> sameTopics = new TreeSet<>(DocRelationComparator.INSTANCE);

        final Set<DocRelation> sameExtensions = new TreeSet<>(DocRelationComparator.INSTANCE);

        public Set<DocRelation> getSameTopics() {
            return sameTopics;
        }

        public Set<DocRelation> getSameExtensions() {
            return sameExtensions;
        }

        @JsonIgnore
        public boolean isEmpty() {
            return sameTopics.isEmpty() && sameExtensions.isEmpty();
        }
    }

    @JsonInclude(value = Include.NON_EMPTY)
    public static class DocRelation {

        final String title;

        final String url;

        final String type;

        final int matches;

        DocRelation(String title, String url, String type, int matches) {
            this.title = title;
            this.url = url;
            this.type = type;
            this.matches = matches;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }

        public int getMatches() {
            return matches;
        }
    }

    public static class DocRelationComparator implements Comparator<DocRelation> {

        static final DocRelationComparator INSTANCE = new DocRelationComparator();

        @Override
        public int compare(DocRelation o1, DocRelation o2) {
            int compareMatches = o2.matches - o1.matches;

            if (compareMatches != 0) {
                return compareMatches;
            }

            return o1.title.compareToIgnoreCase(o2.title);
        }
    }

    @JsonInclude(value = Include.NON_EMPTY)
    public static class FileMessages {
        Collection<String> errors;
        Collection<String> warnings;

        Collection<String> warnings() {
            return warnings == null ? List.of() : warnings;
        }

        Collection<String> errors() {
            return errors == null ? List.of() : errors;
        }

        public boolean listAll(StringBuilder sb) {
            errors().forEach(e -> sb.append("    [ ERR] ").append(e).append("\n"));
            warnings().forEach(e -> sb.append("    [WARN] ").append(e).append("\n"));
            sb.append("\n");
            return !errors().isEmpty();
        }

        public boolean mdListAll(StringBuilder sb) {
            errors().forEach(e -> sb.append("- 🛑 ").append(e).append("\n"));
            warnings().forEach(e -> sb.append("- ⚠️ ").append(e).append("\n"));
            sb.append("\n");
            return !errors().isEmpty();
        }
    }
}
