package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.quarkus.docs.generation.ReferenceIndexGenerator.Index;

/**
 * Iterate over the documents in the source directory and check the cross references.
 */
public class CheckCrossReferences {

    private static final String SOURCE_BLOCK_PREFIX = "[source";
    private static final String SOURCE_BLOCK_DELIMITER = "--";
    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\[]+)\\[[^\\]]*\\]");
    private static final Pattern ANGLE_BRACKETS_WITHOUT_DESCRIPTION_PATTERN = Pattern.compile("<<([a-z0-9_\\-#\\.]+?)>>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANGLE_BRACKETS_WITH_DESCRIPTION_PATTERN = Pattern.compile("<<([a-z0-9_\\-#\\.]+?),([^>]+?)>>",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> IGNORED_GUIDES = Set.of(
            // contains a reference to container-image.adoc#s2i that I don't know how to fix
            "deploying-to-kubernetes.adoc",
            // contains a reference to container-image.adoc#s2i that I don't know how to fix
            "deploying-to-openshift.adoc");

    private final Path srcDir;
    private final Index referenceIndex;

    public static void main(String[] args) throws Exception {
        CheckCrossReferences checker = new CheckCrossReferences(args.length >= 1
                ? Path.of(args[0])
                : docsDir().resolve("src/main/asciidoc"),
                args.length >= 2
                        ? Path.of(args[1])
                        : docsDir().resolve("target/referenceIndex.yaml"));
        System.out.println("[INFO] Checking cross references using: " + args[0]);

        Map<String, List<String>> errors = checker.check();

        if (!errors.isEmpty()) {
            StringBuffer errorLog = new StringBuffer("Unable to find cross reference for:\n\n");

            for (Entry<String, List<String>> errorEntry : errors.entrySet()) {
                errorLog.append("- " + errorEntry.getKey() + "\n");
                for (String error : errorEntry.getValue()) {
                    errorLog.append("    . " + error + "\n");
                }
            }

            throw new IllegalStateException(errorLog.toString());
        }

        System.out.println("[INFO] Done");
    }

    public CheckCrossReferences(Path srcDir, Path referenceIndexPath)
            throws StreamReadException, DatabindException, IOException {
        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IllegalStateException(
                    String.format("Source directory (%s) does not exist", srcDir.toAbsolutePath()));
        }
        this.srcDir = srcDir;

        if (!Files.exists(referenceIndexPath) || !Files.isReadable(referenceIndexPath)) {
            throw new IllegalStateException(
                    String.format("Reference index does not exist or is not readable", referenceIndexPath.toAbsolutePath()));
        }

        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        this.referenceIndex = om.readValue(referenceIndexPath.toFile(), Index.class);
    }

    private Map<String, List<String>> check() throws IOException {
        final Map<String, String> titlesByReference = referenceIndex.getReferences().stream()
                .collect(Collectors.toMap(s -> s.getReference(), s -> s.getTitle()));
        final Map<String, List<String>> errors = new TreeMap<>();

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

                        StringBuilder currentBuffer = new StringBuilder();
                        boolean inSourceBlock = false;
                        boolean findDelimiter = false;
                        String currentSourceBlockDelimiter = "----";
                        int lineNumber = 0;

                        for (String line : guideLines) {
                            lineNumber++;

                            if (inSourceBlock) {
                                if (findDelimiter) {
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
                                continue;
                            }
                            if (line.startsWith(SOURCE_BLOCK_PREFIX)) {
                                inSourceBlock = true;
                                findDelimiter = true;

                                if (currentBuffer.length() > 0) {
                                    checkLinks(titlesByReference, errors, fileName, currentBuffer.toString());
                                    currentBuffer.setLength(0);
                                }
                                continue;
                            }

                            currentBuffer.append(line + "\n");
                        }

                        if (currentBuffer.length() > 0) {
                            checkLinks(titlesByReference, errors, fileName, currentBuffer.toString());
                        }
                    });
        }

        return errors;
    }

    private static void checkLinks(Map<String, String> titlesByReference,
            Map<String, List<String>> errors,
            String fileName,
            String content) {
        Matcher matcher = XREF_PATTERN.matcher(content);
        while (matcher.find()) {
            String reference = getQualifiedReference(fileName, matcher.group(1));
            if (!titlesByReference.containsKey(reference)) {
                addError(errors, fileName, reference + " in link " + matcher.group());
            }
        }

        matcher = ANGLE_BRACKETS_WITHOUT_DESCRIPTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String reference = getQualifiedReference(fileName, matcher.group(1));
            if (!titlesByReference.containsKey(reference)) {
                addError(errors, fileName, reference + " in link " + matcher.group());
            }
        }

        matcher = ANGLE_BRACKETS_WITH_DESCRIPTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String reference = getQualifiedReference(fileName, matcher.group(1));
            if (!titlesByReference.containsKey(reference)) {
                addError(errors, fileName, reference + " in link " + matcher.group());
            }
        }
    }

    private boolean includeFile(String fileName) {
        if (fileName.startsWith("_attributes") || fileName.equals("README.adoc")) {
            return false;
        }
        if (fileName.startsWith("doc-")) {
            // these files are for the doc infrastructure and contain a lot of examples that would be hard to ignore in the checks
            return false;
        }
        if (IGNORED_GUIDES.contains(fileName)) {
            return false;
        }
        if (fileName.endsWith(".adoc")) {
            return true;
        }
        return false;
    }

    private static String getQualifiedReference(String fileName, String reference) {
        reference = normalizeAdoc(reference);

        if (reference.startsWith("#")) {
            return fileName + reference;
        }

        if (reference.contains(".adoc")) {
            return reference;
        }

        if (reference.contains("#")) {
            int hashIndex = reference.indexOf('#');
            return reference.substring(0, hashIndex) + ".adoc" + reference.substring(hashIndex);
        }

        return fileName + "#" + reference;
    }

    private static String normalizeAdoc(String adoc) {
        if (adoc.startsWith("./")) {
            return adoc.substring(2);
        }

        return adoc;
    }

    private static void addError(Map<String, List<String>> errors, String fileName, String error) {
        errors.computeIfAbsent(fileName, f -> new ArrayList<>())
                .add(error);
    }

    private static Path docsDir() {
        Path path = Paths.get(System.getProperty("user.dir"));
        if (path.endsWith("docs")) {
            return path;
        }
        return path.resolve("docs");
    }
}
