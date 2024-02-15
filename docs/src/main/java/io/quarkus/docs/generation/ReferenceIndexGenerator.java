package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Iterate over the documents in the source directory and create an index of the references.
 */
public class ReferenceIndexGenerator {

    private static final String YAML_FRONTMATTER = "---\n";
    private static final String SOURCE_BLOCK_PREFIX = "[source";
    private static final String SOURCE_BLOCK_DELIMITER = "--";

    private static final Pattern ICON_PATTERN = Pattern.compile("icon:[^\\[]+\\[[^\\]]*\\] ");

    private final Path srcDir;
    private final Path targetDir;

    public static void main(String[] args) throws Exception {
        System.out.println("[INFO] Creating reference index generator: " + List.of(args));
        ReferenceIndexGenerator generator = new ReferenceIndexGenerator(args.length >= 1
                ? Path.of(args[0])
                : docsDir().resolve("src/main/asciidoc"),
                args.length >= 2
                        ? Path.of(args[1])
                        : docsDir().resolve("target"));

        System.out.println("[INFO] Generating reference index");
        Index index = generator.generateIndex();
        System.out.println("[INFO] Writing reference index file");
        generator.writeYamlFiles(index);
        System.out.println("[INFO] Transforming the source code");
        Map<String, List<String>> errors = generator.transformFiles(index);

        if (!errors.isEmpty()) {
            System.out.println();
            System.out.println("################################################");
            System.out.println("# Errors occurred while transforming references");
            System.out.println("################################################");
            System.out.println();

            for (Entry<String, List<String>> errorEntry : errors.entrySet()) {
                System.out.println("- " + errorEntry.getKey());
                for (String error : errorEntry.getValue()) {
                    System.out.println("    . " + error);
                }
            }

            System.out.println();
            System.exit(1);
        }

        System.out.println("[INFO] Done");
    }

    public ReferenceIndexGenerator(Path srcDir, Path targetDir) {
        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist", srcDir.toAbsolutePath()));
        }
        this.srcDir = srcDir;

        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist", srcDir.toAbsolutePath()));
        }

        this.targetDir = targetDir;
    }

    private void writeYamlFiles(Index index) throws StreamWriteException, DatabindException, IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

        om.writeValue(targetDir.resolve("referenceIndex.yaml").toFile(), index);
    }

    private Index generateIndex() throws IOException {
        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist", srcDir.toAbsolutePath()));
        }
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            throw new IllegalStateException(
                    String.format("Target directory (%s) does not exist. Exiting.%n", targetDir.toAbsolutePath()));
        }

        Options options = Options.builder()
                .docType("book")
                .sourceDir(srcDir.toFile())
                .baseDir(srcDir.toFile())
                .safe(SafeMode.UNSAFE)
                .build();

        Index index = new Index();

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            try (Stream<Path> pathStream = Files.list(srcDir)) {
                pathStream.filter(path -> includeFile(path.getFileName().toString()))
                        .forEach(path -> {
                            String guideContent;
                            try {
                                guideContent = Files.readString(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }

                            // Strip off YAML frontmatter, if present
                            if (guideContent.startsWith(YAML_FRONTMATTER)) {
                                int end = guideContent.indexOf(YAML_FRONTMATTER, YAML_FRONTMATTER.length());
                                guideContent = guideContent.substring(end + YAML_FRONTMATTER.length());
                            }

                            Document doc = asciidoctor.load(guideContent, options);

                            String fileName = path.getFileName().toString();
                            String title = doc.getDoctitle();

                            index.add(new IndexReference(fileName, title));

                            addBlocks(index, fileName, doc.getBlocks());
                        });
            }
        }

        return index;
    }

    private void addBlocks(Index index, String fileName, List<StructuralNode> blocks) {
        for (StructuralNode block : blocks) {
            if (block instanceof Section || block instanceof Table || block instanceof Block) {
                if (block.getId() != null) {
                    // unfortunately, AsciiDoc already formats the title in the AST
                    // and I couldn't find a way to get the original one
                    IndexReference reference = new IndexReference(fileName, block.getId(),
                            block.getTitle() != null ? block.getTitle().replace("<code>", "`")
                                    .replace("</code>", "`")
                                    .replace('\n', ' ')
                                    .replace("&#8217;", "'")
                                    .replace("&amp;", "&")
                                    .replace("&#8230;&#8203;", "...")
                                    .replace("<em>", "_")
                                    .replace("</em>", "_")
                                    .replace("<b>", "*")
                                    .replace("</b>", "*")
                                    .replace("<strong>", "*")
                                    .replace("</strong>", "*")
                                    .replaceAll("<a.*</a> ", "") : "~~ unknown title ~~");

                    index.add(reference);
                }
            }
            // we go into the content of the tables to add references for the configuration properties
            if (block instanceof Table) {
                for (Row row : ((Table) block).getBody()) {
                    for (Cell cell : row.getCells()) {
                        String cellContent = ICON_PATTERN.matcher(cell.getSource()).replaceAll("").trim();

                        if (!cellContent.startsWith("[[") || !cellContent.contains("]]")) {
                            continue;
                        }

                        IndexReference reference = new IndexReference(fileName,
                                cellContent.substring(2, cellContent.indexOf("]]")),
                                "Configuration property documentation");
                        index.add(reference);
                    }
                }
            }
            addBlocks(index, fileName, block.getBlocks());
        }
    }

    private Map<String, List<String>> transformFiles(Index index) throws IOException {
        final Map<String, String> titlesByReference = index.getReferences().stream()
                .collect(Collectors.toMap(s -> s.getReference(), s -> s.getTitle()));
        final Map<String, List<String>> errors = new LinkedHashMap<>();

        try (Stream<Path> pathStream = Files.list(srcDir)) {
            pathStream.filter(path -> includeFile(path.getFileName().toString()))
                    .forEach(path -> {
                        List<String> guideLines;
                        try {
                            guideLines = Files.readAllLines(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }

                        String fileName = path.getFileName().toString();

                        StringBuilder rewrittenGuide = new StringBuilder();
                        StringBuilder currentBuffer = new StringBuilder();
                        boolean inSourceBlock = false;
                        boolean findDelimiter = false;
                        String currentSourceBlockDelimiter = "----";
                        int lineNumber = 0;

                        for (String line : guideLines) {
                            lineNumber++;

                            if (inSourceBlock) {
                                if (findDelimiter) {
                                    rewrittenGuide.append(line + "\n");
                                    if (line.isBlank() || line.startsWith(".")) {
                                        continue;
                                    }
                                    if (!line.startsWith(SOURCE_BLOCK_DELIMITER)) {
                                        throw new IllegalStateException("Unable to find source block delimiter in file "
                                                + fileName + " at line " + lineNumber);
                                    }
                                    currentSourceBlockDelimiter = line.stripTrailing();
                                    findDelimiter = false;
                                    continue;
                                }

                                if (line.stripTrailing().equals(currentSourceBlockDelimiter)) {
                                    inSourceBlock = false;
                                }
                                rewrittenGuide.append(line + "\n");
                                continue;
                            }
                            if (line.startsWith(SOURCE_BLOCK_PREFIX)) {
                                inSourceBlock = true;
                                findDelimiter = true;

                                if (currentBuffer.length() > 0) {
                                    rewrittenGuide.append(
                                            rewriteLinks(titlesByReference, errors, fileName, currentBuffer.toString()));
                                    currentBuffer.setLength(0);
                                }
                                rewrittenGuide.append(line + "\n");
                                continue;
                            }

                            currentBuffer.append(line + "\n");
                        }

                        if (currentBuffer.length() > 0) {
                            rewrittenGuide
                                    .append(rewriteLinks(titlesByReference, errors, fileName, currentBuffer.toString()));
                        }

                        try {
                            Files.writeString(path, rewrittenGuide);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        return errors;
    }

    private String rewriteLinks(Map<String, String> titlesByReference,
            Map<String, List<String>> errors,
            String fileName,
            String content) {
        // we don't do anything here from now, this code was moved to AssembleDownstreamDocumentation

        return content;
    }

    private boolean includeFile(String fileName) {
        if (fileName.startsWith("_attributes") || fileName.equals("README.adoc")) {
            return false;
        }
        if (fileName.endsWith(".adoc")) {
            return true;
        }
        return false;
    }

    private static Path docsDir() {
        Path path = Paths.get(System.getProperty("user.dir"));
        if (path.endsWith("docs")) {
            return path;
        }
        return path.resolve("docs");
    }

    public static class Index {

        private List<IndexReference> references = new ArrayList<>();

        public List<IndexReference> getReferences() {
            return references;
        }

        public void add(IndexReference reference) {
            references.add(reference);
        }
    }

    public static class IndexReference {

        private String reference;
        private String title;

        public IndexReference() {
        }

        public IndexReference(String fileName, String title) {
            this.reference = fileName;
            this.title = title;
        }

        public IndexReference(String fileName, String blockId, String title) {
            this.reference = fileName + "#" + blockId;
            this.title = title;
        }

        public String getReference() {
            return reference;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return reference + " -> " + title;
        }
    }
}
