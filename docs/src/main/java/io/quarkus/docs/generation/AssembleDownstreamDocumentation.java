package io.quarkus.docs.generation;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
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
    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\.#\\[ ]+)\\" + ADOC_SUFFIX);
    private static final String SOURCE_BLOCK_PREFIX = "[source";
    private static final String SOURCE_BLOCK_DELIMITER = "--";

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

        try {
            deleteDirectory(TARGET_ROOT_DIRECTORY);
            Files.deleteIfExists(TARGET_LISTING);

            ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
            yamlObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            ConfigFile configFile = yamlObjectMapper.readValue(new File("downstreamdoc.yaml"), ConfigFile.class);

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
                copyAsciidoc(guide, TARGET_ROOT_DIRECTORY.resolve(guide.getFileName()), downstreamGuides);
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
                copyAsciidoc(sourceFile, targetFile, downstreamGuides);
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
                copyAsciidoc(sourceFile, targetFile, downstreamGuides);
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
                copyAsciidoc(sourceFile, targetFile, downstreamGuides);
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

    private static void copyAsciidoc(Path sourceFile, Path targetFile, Set<String> downstreamGuides) throws IOException {
        List<String> guideLines = Files.readAllLines(sourceFile);

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
                            rewriteLinks(currentBuffer.toString(), downstreamGuides));
                    currentBuffer.setLength(0);
                }
                rewrittenGuide.append(line + "\n");
                continue;
            }

            currentBuffer.append(line + "\n");
        }

        if (currentBuffer.length() > 0) {
            rewrittenGuide
                    .append(rewriteLinks(currentBuffer.toString(), downstreamGuides));
        }

        String rewrittenGuideWithoutTabs = rewrittenGuide.toString().trim();

        for (Entry<Pattern, String> tabReplacement : TABS_REPLACEMENTS.entrySet()) {
            rewrittenGuideWithoutTabs = tabReplacement.getKey().matcher(rewrittenGuideWithoutTabs)
                    .replaceAll(tabReplacement.getValue());
        }

        Files.writeString(targetFile, rewrittenGuideWithoutTabs.trim());
    }

    private static String rewriteLinks(String content, Set<String> downstreamGuides) {
        content = XREF_PATTERN.matcher(content).replaceAll(mr -> {
            if (downstreamGuides.contains(mr.group(1) + ADOC_SUFFIX)) {
                return mr.group(0);
            }

            return "link:" + QUARKUS_IO_GUIDES_ATTRIBUTE + "/" + mr.group(1);
        });

        return content;
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
