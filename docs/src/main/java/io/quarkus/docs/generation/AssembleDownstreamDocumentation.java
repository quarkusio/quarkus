package io.quarkus.docs.generation;

//These are here to allow running the script directly from command line/IDE
//The real deps and call are in the pom.xml
//DEPS org.jboss.logging:jboss-logging:3.4.1.Final
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.0.rc1
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.quarkus.docs.generation.ReferenceIndexGenerator.Index;

public class AssembleDownstreamDocumentation {

    private static final Logger LOG = Logger.getLogger(AssembleDownstreamDocumentation.class);

    private static final Path SOURCE_DOC_PATH = Path.of("src", "main", "asciidoc");
    private static final Path DOC_PATH = Path.of("target", "asciidoc", "sources");
    private static final Path INCLUDES_PATH = DOC_PATH.resolve("_includes");
    private static final Path GENERATED_FILES_PATH = Path.of("..", "target", "asciidoc", "generated");
    private static final Path IMAGES_PATH = DOC_PATH.resolve("images");
    private static final Path TARGET_ROOT_DIRECTORY = Path.of("target", "downstream-tree");
    private static final Path TARGET_IMAGES_DIRECTORY = TARGET_ROOT_DIRECTORY.resolve("images");
    private static final Path TARGET_INCLUDES_DIRECTORY = TARGET_ROOT_DIRECTORY.resolve("_includes");
    private static final Path TARGET_GENERATED_DIRECTORY = TARGET_ROOT_DIRECTORY.resolve("_generated");
    private static final Path TARGET_LISTING = Path.of("target", "downstream-files.txt");
    private static final Set<Path> EXCLUDED_FILES = Set.of(
            DOC_PATH.resolve("_attributes-local.adoc"));

    private static final String ADOC_SUFFIX = ".adoc";
    private static final Pattern XREF_GUIDE_PATTERN = Pattern.compile("xref:([^\\.#\\[ ]+)\\" + ADOC_SUFFIX);
    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\[]+)\\[]");
    private static final Pattern ANGLE_BRACKETS_WITHOUT_DESCRIPTION_PATTERN = Pattern.compile("<<([a-z0-9_\\-#\\.]+?)>>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANGLE_BRACKETS_WITH_DESCRIPTION_PATTERN = Pattern.compile("<<([a-z0-9_\\-#\\.]+?),([^>]+?)>>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("^\\[#([a-z0-9_-]+)]$",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
    private static final String SOURCE_BLOCK_PREFIX = "[source";
    private static final String SOURCE_BLOCK_DELIMITER = "--";

    private static final String PROJECT_NAME_ATTRIBUTE = "{project-name}";
    private static final String RED_HAT_BUILD_OF_QUARKUS = "Red Hat build of Quarkus";

    private static final String QUARKUS_IO_GUIDES_ATTRIBUTE = "{quarkusio-guides}";

    private static final Map<Pattern, String> TABS_REPLACEMENTS = Map.of(
            Pattern.compile(
                    "((\\*) [^\n]+\n\\+)?\n\\[source,\\s?xml,\\s?role=\"primary asciidoc-tabs-target-sync-cli asciidoc-tabs-target-sync-maven\"\\]\n\\.pom.xml\n----\n((([^-]+\\-?)+\n)+?)----\n(\\+?)\n\\[source,\\s?gradle,\\s?role=\"secondary asciidoc-tabs-target-sync-gradle\"\\]\n\\.build.gradle\n----\n((([^-]+\\-?)+\n)+?)----",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            "$1\n$2* Using Maven:\n+\n--\n[source,xml]\n----\n$3----\n--\n+\n$2* Using Gradle:\n+\n--\n[source,gradle]\n----\n$7----\n--",
            Pattern.compile(
                    "\\[source,\\s?bash,\\s?subs=attributes\\+,\\s?role=\"primary asciidoc-tabs-sync-cli\"\\]\n\\.CLI\n(----)\n((([^-]+\\-?\\-?)+\n)+?)(----)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            "* Using the Quarkus CLI:\n+\n--\n[source, bash, subs=attributes+]\n----\n$2----\n--",
            Pattern.compile(
                    "\\[source,\\s?bash,\\s?subs=attributes\\+,\\s?role=\"secondary asciidoc-tabs-sync-maven\"\\]\n\\.Maven\n(----)\n((([^-]+\\-?\\-?)+\n)+?)(----)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            "* Using Maven:\n+\n--\n[source, bash, subs=attributes+]\n----\n$2----\n--",
            Pattern.compile(
                    "\\[source,\\s?bash,\\s?subs=attributes\\+,\\s?role=\"secondary asciidoc-tabs-sync-gradle\"\\]\n\\.Gradle\n(----)\n((([^-]+\\-?\\-?)+\n)+?)(----)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            "* Using Gradle:\n+\n--\n[source, bash, subs=attributes+]\n----\n$2----\n--",
            Pattern.compile(
                    "\\[role=\"primary\\s?asciidoc-tabs-sync-cli\"\\]\n\\.CLI\n\\*\\*\\*\\*\n\\[source,\\s?bash,\\s?subs=attributes\\+\\]\n----\n((([^-]+\\-?\\-?)+\n)+?)----\n((([^*]+\\*?\\*?)+\n)+?)\\*\\*\\*\\*",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            "* Using the Quarkus CLI:\n+\n--\n[source, bash, subs=attributes+]\n----\n$1----\n$4--",
            Pattern.compile(
                    "\\[role=\"secondary\\s?asciidoc-tabs-sync-maven\"\\]\n\\.Maven\n\\*\\*\\*\\*\n\\[source,\\s?bash,\\s?subs=attributes\\+\\]\n----\n((([^-]+\\-?\\-?)+\n)+?)----\n((([^*]+\\*?\\*?)+\n)+?)\\*\\*\\*\\*",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            "* Using Maven:\n+\n--\n[source, bash, subs=attributes+]\n----\n$1----\n$4--");

    public static void main(String[] args) throws Exception {
        if (!Files.isDirectory(DOC_PATH)) {
            throw new IllegalStateException(
                    "Transformed AsciiDoc sources directory does not exist. Have you built the documentation?");
        }
        if (!Files.isDirectory(GENERATED_FILES_PATH)) {
            throw new IllegalStateException("Generated files directory does not exist. Have you built the documentation?");
        }
        Path referenceIndexPath = Path.of(args[0]);
        if (!Files.isReadable(Path.of(args[0]))) {
            throw new IllegalStateException("Reference index does not exist? Have you built the documentation?");
        }

        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        Index referenceIndex = om.readValue(referenceIndexPath.toFile(), Index.class);

        Map<String, List<String>> linkRewritingErrors = new LinkedHashMap<>();
        Map<String, String> titlesByReference = referenceIndex.getReferences().stream()
                .collect(Collectors.toMap(s -> s.getReference(), s -> s.getTitle()));

        try {
            deleteDirectory(TARGET_ROOT_DIRECTORY);
            Files.deleteIfExists(TARGET_LISTING);

            ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
            yamlObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            String configFilePath = System.getenv("DOWNSTREAM_CONFIG_FILE");
            if (configFilePath == null) {
                configFilePath = "downstreamdoc.yaml";
            }
            ConfigFile configFile = yamlObjectMapper.readValue(new File(configFilePath), ConfigFile.class);

            String additionals = System.getenv("DOWNSTREAM_ADDITIONALS");
            if (additionals != null) {
                String[] additional_files = additionals.split(",");
                LOG.info("Additional files: " + Arrays.toString(additional_files));
                for (String file : additional_files) {
                    configFile.guides.add(file);
                }
            }

            String excludes = System.getenv("DOWNSTREAM_EXCLUDES");
            if (excludes != null) {
                String[] excludePatterns = excludes.split(",");
                LOG.info("Excluding patterns: " + Arrays.toString(excludePatterns));
                for (String pattern : excludePatterns) {
                    Pattern regexPattern = Pattern.compile(pattern);
                    configFile.guides.removeIf(guide -> regexPattern.matcher(guide).find());
                }
            }

            Set<Path> guides = new TreeSet<>();
            Set<Path> simpleIncludes = new TreeSet<>();
            Set<Path> includes = new TreeSet<>();
            Set<Path> generatedFiles = new TreeSet<>();
            Set<Path> images = new TreeSet<>();

            Set<Path> allResolvedPaths = new TreeSet<>();

            Set<String> downstreamGuides = new TreeSet<>();

            for (String guide : new TreeSet<>(configFile.guides)) {
                Path guidePath = DOC_PATH.resolve(SOURCE_DOC_PATH.relativize(Path.of(guide)));

                if (!Files.isReadable(guidePath)) {
                    LOG.error("Unable to read file " + guidePath);
                    continue;
                }

                downstreamGuides.add(guidePath.getFileName().toString());
                allResolvedPaths.add(guidePath);

                GuideContent guideContent = new GuideContent(guidePath);
                getFiles(guideContent, guidePath);

                guides.add(guidePath);
                simpleIncludes.addAll(guideContent.simpleIncludes);
                includes.addAll(guideContent.includes);
                generatedFiles.addAll(guideContent.generatedFiles);
                images.addAll(guideContent.images);
            }

            Files.createDirectories(TARGET_ROOT_DIRECTORY);

            for (Path guide : guides) {
                System.out.println("[INFO] Processing guide " + guide.getFileName());
                copyAsciidoc(guide, TARGET_ROOT_DIRECTORY.resolve(guide.getFileName()), downstreamGuides, titlesByReference,
                        linkRewritingErrors);
            }
            for (Path simpleInclude : simpleIncludes) {
                Path sourceFile = DOC_PATH.resolve(simpleInclude);

                if (EXCLUDED_FILES.contains(sourceFile)) {
                    continue;
                }
                if (!Files.isReadable(sourceFile)) {
                    LOG.error("Unable to read include " + sourceFile);
                }
                allResolvedPaths.add(sourceFile);
                Path targetFile = TARGET_ROOT_DIRECTORY.resolve(simpleInclude);
                Files.createDirectories(targetFile.getParent());
                copyAsciidoc(sourceFile, targetFile, downstreamGuides, titlesByReference, linkRewritingErrors);
            }
            for (Path include : includes) {
                Path sourceFile = INCLUDES_PATH.resolve(include);
                if (EXCLUDED_FILES.contains(sourceFile)) {
                    continue;
                }
                if (!Files.isReadable(sourceFile)) {
                    LOG.error("Unable to read include " + sourceFile);
                }
                allResolvedPaths.add(sourceFile);
                Path targetFile = TARGET_INCLUDES_DIRECTORY.resolve(include);
                Files.createDirectories(targetFile.getParent());
                copyAsciidoc(sourceFile, targetFile, downstreamGuides, titlesByReference, linkRewritingErrors);
            }
            for (Path generatedFile : generatedFiles) {
                Path sourceFile = GENERATED_FILES_PATH.resolve(generatedFile);
                if (EXCLUDED_FILES.contains(sourceFile)) {
                    continue;
                }
                if (!Files.isReadable(sourceFile)) {
                    LOG.error("Unable to read generated file " + sourceFile);
                }
                allResolvedPaths.add(sourceFile);
                Path targetFile = TARGET_GENERATED_DIRECTORY.resolve(generatedFile);
                Files.createDirectories(targetFile.getParent());
                copyAsciidoc(sourceFile, targetFile, downstreamGuides, titlesByReference, linkRewritingErrors);
            }
            for (Path image : images) {
                Path sourceFile = IMAGES_PATH.resolve(image);
                if (EXCLUDED_FILES.contains(sourceFile)) {
                    continue;
                }
                if (!Files.isReadable(sourceFile)) {
                    LOG.error("Unable to read image " + sourceFile);
                }
                allResolvedPaths.add(sourceFile);
                Path targetFile = TARGET_IMAGES_DIRECTORY.resolve(image);
                Files.createDirectories(targetFile.getParent());
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.writeString(TARGET_LISTING,
                    allResolvedPaths.stream().map(p -> p.toString()).collect(Collectors.joining("\n")));

            if (!linkRewritingErrors.isEmpty()) {
                System.out.println();
                System.out.println("################################################");
                System.out.println("# Errors occurred while transforming references");
                System.out.println("################################################");
                System.out.println();

                for (Entry<String, List<String>> errorEntry : linkRewritingErrors.entrySet()) {
                    System.out.println("- " + errorEntry.getKey());
                    for (String error : errorEntry.getValue()) {
                        System.out.println("    . " + error);
                    }
                }

                System.out.println();
                System.exit(1);
            }

            LOG.info("Downstream documentation tree is available in: " + TARGET_ROOT_DIRECTORY);
            LOG.info("Downstream documentation listing is available in: " + TARGET_LISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("An error occurred while generating the downstream tree", e);
        }
    }

    private static void getFiles(GuideContent guideContent, Path currentFile) throws IOException {
        List<String> lines = Files.readAllLines(currentFile);

        for (String line : lines) {
            Optional<Path> possibleInclude = extractPath(line, "include::{includes}");
            if (possibleInclude.isPresent()) {
                guideContent.includes.add(possibleInclude.get());
                getFurtherIncludes(guideContent, INCLUDES_PATH.resolve(possibleInclude.get()));
                continue;
            }
            Optional<Path> possibleGeneratedFile = extractPath(line, "include::{generated-dir}");
            if (possibleGeneratedFile.isPresent()) {
                guideContent.generatedFiles.add(possibleGeneratedFile.get());
                continue;
            }
            Optional<Path> possibleSimpleInclude = extractPath(line, "include::");
            if (possibleSimpleInclude.isPresent()) {
                guideContent.simpleIncludes.add(possibleSimpleInclude.get());
                getFiles(guideContent, currentFile.getParent().resolve(possibleSimpleInclude.get()));
                continue;
            }
            Optional<Path> possibleImage = extractPath(line, "image::");
            if (possibleImage.isPresent()) {
                guideContent.images.add(possibleImage.get());
                continue;
            }
        }
    }

    private static void getFurtherIncludes(GuideContent guideContent, Path currentFile) throws IOException {
        List<String> lines = Files.readAllLines(currentFile);

        for (String line : lines) {
            Optional<Path> possibleInclude = extractPath(line, "include::");
            if (possibleInclude.isPresent()) {
                guideContent.includes.add(possibleInclude.get());
                getFurtherIncludes(guideContent, currentFile.getParent().resolve(possibleInclude.get()));
                continue;
            }
            Optional<Path> possibleImage = extractPath(line, "image::");
            if (possibleImage.isPresent()) {
                guideContent.images.add(possibleImage.get());
                continue;
            }
        }
    }

    private static Optional<Path> extractPath(String asciidoc, String prefix) {
        if (!asciidoc.startsWith(prefix)) {
            return Optional.empty();
        }

        String path = asciidoc.substring(prefix.length(), asciidoc.indexOf('['));

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return Optional.of(Path.of(path));
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }

        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static void copyAsciidoc(Path sourceFile, Path targetFile, Set<String> downstreamGuides,
            Map<String, String> titlesByReference,
            Map<String, List<String>> linkRewritingErrors) throws IOException {
        List<String> guideLines = Files.readAllLines(sourceFile);

        StringBuilder rewrittenGuide = new StringBuilder();
        StringBuilder currentBuffer = new StringBuilder();
        boolean inSourceBlock = false;
        boolean findDelimiter = false;
        String currentSourceBlockDelimiter = "----";
        int lineNumber = 0;
        boolean documentTitleFound = false;

        for (String line : guideLines) {
            lineNumber++;

            if (!documentTitleFound && line.startsWith("= ")) {
                // this is the document title
                rewrittenGuide.append(line.replace(PROJECT_NAME_ATTRIBUTE, RED_HAT_BUILD_OF_QUARKUS) + "\n");
                documentTitleFound = true;
                continue;
            }
            if (inSourceBlock) {
                if (findDelimiter) {
                    rewrittenGuide.append(line + "\n");
                    if (line.isBlank() || line.startsWith(".")) {
                        continue;
                    }
                    if (!line.startsWith(SOURCE_BLOCK_DELIMITER)) {
                        throw new IllegalStateException("Unable to find source block delimiter in file "
                                + sourceFile + " at line " + lineNumber);
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
                            rewriteLinks(sourceFile.getFileName().toString(), currentBuffer.toString(), downstreamGuides,
                                    titlesByReference, linkRewritingErrors));
                    currentBuffer.setLength(0);
                }
                rewrittenGuide.append(line + "\n");
                continue;
            }

            currentBuffer.append(line + "\n");
        }

        if (currentBuffer.length() > 0) {
            rewrittenGuide.append(
                    rewriteLinks(sourceFile.getFileName().toString(), currentBuffer.toString(), downstreamGuides,
                            titlesByReference, linkRewritingErrors));
        }

        String rewrittenGuideWithoutTabs = rewrittenGuide.toString().trim();

        for (Entry<Pattern, String> tabReplacement : TABS_REPLACEMENTS.entrySet()) {
            rewrittenGuideWithoutTabs = tabReplacement.getKey().matcher(rewrittenGuideWithoutTabs)
                    .replaceAll(tabReplacement.getValue());
        }

        Files.writeString(targetFile, rewrittenGuideWithoutTabs.trim());
    }

    private static String rewriteLinks(String fileName,
            String content,
            Set<String> downstreamGuides,
            Map<String, String> titlesByReference,
            Map<String, List<String>> errors) {
        content = XREF_PATTERN.matcher(content).replaceAll(mr -> {
            String reference = getQualifiedReference(fileName, mr.group(1));
            String title = titlesByReference.get(reference);
            if (title == null || title.isBlank()) {
                addError(errors, fileName, "Unable to find title for: " + mr.group() + " [" + reference + "]");
                title = "~~ unknown title ~~";
            }
            return "xref:" + trimReference(mr.group(1)) + "[" + title.trim() + "]";
        });

        content = ANGLE_BRACKETS_WITHOUT_DESCRIPTION_PATTERN.matcher(content).replaceAll(mr -> {
            String reference = getQualifiedReference(fileName, mr.group(1));
            String title = titlesByReference.get(reference);
            if (title == null || title.isBlank()) {
                addError(errors, fileName, "Unable to find title for: " + mr.group() + " [" + reference + "]");
                title = "~~ unknown title ~~";
            }
            return "xref:" + trimReference(mr.group(1)) + "[" + title.trim() + "]";
        });

        content = ANGLE_BRACKETS_WITH_DESCRIPTION_PATTERN.matcher(content).replaceAll(mr -> {
            return "xref:" + trimReference(mr.group(1)) + "[" + mr.group(2).trim() + "]";
        });

        content = XREF_GUIDE_PATTERN.matcher(content).replaceAll(mr -> {
            if (downstreamGuides.contains(mr.group(1) + ADOC_SUFFIX)) {
                return mr.group(0);
            }

            return "link:" + QUARKUS_IO_GUIDES_ATTRIBUTE + "/" + mr.group(1);
        });

        content = ANCHOR_PATTERN.matcher(content).replaceAll(mr -> {
            return "[[" + mr.group(1) + "]]";
        });

        return content;
    }

    private static String trimReference(String reference) {
        reference = normalizeAdoc(reference);

        if (reference.startsWith("#")) {
            return reference.substring(1);
        }

        if (reference.contains(".adoc")) {
            return reference;
        }

        if (reference.contains("#")) {
            int hashIndex = reference.indexOf('#');
            return reference.substring(0, hashIndex) + ".adoc" + reference.substring(hashIndex);
        }

        return reference;
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

    public static class GuideContent {

        public Path guide;
        public Set<Path> simpleIncludes = new TreeSet<>();
        public Set<Path> includes = new TreeSet<>();
        public Set<Path> images = new TreeSet<>();
        public Set<Path> generatedFiles = new TreeSet<>();

        public GuideContent(Path guide) {
            this.guide = guide;
        }
    }

    public static class ConfigFile {

        public List<String> guides;
    }
}
