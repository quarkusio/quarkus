package io.quarkus.docs.generation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Copies sources declared by {@code *-examples.yaml} manifests into the generated documentation tree.
 * <p>
 * All configured manifest roots share one target namespace. Manifest discovery and validation diagnostics are
 * deterministic so that local and CI failures identify the same conflicting declarations.
 */
public class CopyExampleSource {

    static Path docsDir() {
        Path path = Paths.get(System.getProperty("user.dir"));
        if (path.endsWith("docs")) {
            return path;
        }
        return path.resolve("docs");
    }

    public Path outputPath;
    public Path rootPath;
    public List<Path> srcPaths;

    // Three arguments: <target output directory> <root source directory> <directory containing *-examples.yaml>
    // ${project.build.directory}/quarkus-generated-doc/examples ${maven.multiModuleProjectDirectory}
    // ${project.basedir}/src/main/asciidoc
    public static void main(String[] args) throws Exception {
        CopyExampleSource copyExamples = new CopyExampleSource();

        // Required first parameter: Target output directory
        if (args.length < 1) {
            System.err.println("Must specify target output directory");
            System.exit(1);
        }
        copyExamples.outputPath = Path.of(args[0]).normalize();
        System.out.println("[INFO] Output directory: " + copyExamples.outputPath);

        // Optional second parameter: Project root directory
        if (args.length > 1) {
            copyExamples.rootPath = Path.of(args[1]).normalize();
        } else {
            copyExamples.rootPath = docsDir().resolve("..").normalize();
        }
        System.out.println("[INFO] Project root: " + copyExamples.rootPath);

        // third parameter and on .. source paths
        if (args.length > 2) {
            copyExamples.srcPaths = Arrays.stream(args).skip(2)
                    .map(x -> Path.of(x).normalize())
                    .collect(Collectors.toList());
        } else {
            copyExamples.srcPaths = List.of(docsDir().resolve("src/main/asciidoc").normalize());
        }

        try {
            copyExamples.run();
        } catch (Exception e) {
            System.err.println("Exception occurred while trying to copy examples");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() throws Exception {
        Files.createDirectories(outputPath);
        YAMLMapper om = YAMLMapper.builder(
                YAMLFactory.builder().enable(YAMLWriteFeature.MINIMIZE_QUOTES).build())
                .changeDefaultVisibility(vc -> vc.withFieldVisibility(Visibility.ANY))
                .build();

        List<ExampleMapping> mappings = new ArrayList<>();
        Path normalizedRootPath = rootPath.toAbsolutePath().normalize();
        Path normalizedOutputPath = outputPath.toAbsolutePath().normalize();
        for (Path manifestPath : discoverManifests()) {
            System.out.println("[INFO] Reading: " + manifestPath);
            MappingList manifest = om.readValue(manifestPath.toFile(), MappingList.class);
            for (Example example : manifest.examples) {
                Path relativeSourcePath = Path.of(example.source).normalize();
                boolean validSource = !relativeSourcePath.isAbsolute()
                        && !relativeSourcePath.toString().isBlank()
                        && !relativeSourcePath.startsWith("..");
                Path sourcePath = normalizedRootPath.resolve(relativeSourcePath).normalize();
                validSource &= sourcePath.startsWith(normalizedRootPath);
                Path relativeTargetPath = Path.of(example.target).normalize();
                boolean validTarget = !relativeTargetPath.isAbsolute()
                        && !relativeTargetPath.toString().isBlank()
                        && !relativeTargetPath.startsWith("..");
                Path targetPath = normalizedOutputPath.resolve(relativeTargetPath).normalize();
                validTarget &= targetPath.startsWith(normalizedOutputPath);
                String targetKey = relativeTargetPath.toString()
                        .replace('\\', '/')
                        .toLowerCase(Locale.ROOT);
                mappings.add(new ExampleMapping(
                        manifestPath,
                        example.source,
                        sourcePath,
                        validSource,
                        example.target,
                        targetKey,
                        targetPath,
                        validTarget));
            }
        }

        // Reserve every target before copying. A missing first mapping must not let a later duplicate take its place.
        Map<String, ExampleMapping> firstMappingsByTarget = new LinkedHashMap<>();
        Map<String, List<ExampleMapping>> mappingsByTarget = new TreeMap<>();
        List<ExampleMapping> missingSources = new ArrayList<>();
        List<ExampleMapping> invalidSources = new ArrayList<>();
        List<ExampleMapping> invalidTargets = new ArrayList<>();
        for (ExampleMapping mapping : mappings) {
            if (mapping.validTarget()) {
                firstMappingsByTarget.putIfAbsent(mapping.targetKey(), mapping);
                mappingsByTarget.computeIfAbsent(mapping.targetKey(), ignored -> new ArrayList<>()).add(mapping);
            } else {
                invalidTargets.add(mapping);
            }
            if (!mapping.validSource()) {
                invalidSources.add(mapping);
            } else if (!Files.exists(mapping.sourcePath())) {
                missingSources.add(mapping);
            }
        }

        for (ExampleMapping mapping : firstMappingsByTarget.values()) {
            if (mapping.validSource() && Files.exists(mapping.sourcePath())) {
                copy(mapping);
            }
        }

        List<List<ExampleMapping>> duplicateTargets = mappingsByTarget.values().stream()
                .filter(targetMappings -> targetMappings.size() > 1)
                .toList();
        if (!missingSources.isEmpty() || !invalidSources.isEmpty() || !invalidTargets.isEmpty()
                || !duplicateTargets.isEmpty()) {
            throw new IllegalStateException(formatErrors(missingSources, invalidSources, invalidTargets, duplicateTargets));
        }
    }

    private List<Path> discoverManifests() throws IOException {
        List<Path> manifests = new ArrayList<>();
        for (Path sourcePath : srcPaths) {
            try (Stream<Path> paths = Files.walk(sourcePath)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith("-examples.yaml"))
                        .map(Path::normalize)
                        .forEach(manifests::add);
            }
        }
        return manifests.stream()
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .toList();
    }

    private static void copy(ExampleMapping mapping) throws IOException {
        Files.createDirectories(mapping.targetPath().getParent());

        // Copy the source file to the target file and replace {{source}} in source-reference comments.
        try (BufferedReader reader = Files.newBufferedReader(mapping.sourcePath(), StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(mapping.targetPath(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("// Source: {{source}}")) {
                    writer.write("// Source: " + mapping.source());
                } else if (line.startsWith("# Source: {{source}}")) {
                    writer.write("# Source: " + mapping.source());
                } else {
                    writer.write(line);
                }
                writer.newLine();
            }
            System.out.printf("[INFO] Copied %s %n        to %s%n", mapping.source(), mapping.targetPath());
        } catch (IOException ioe) {
            System.err.printf("[ERROR] Error copying %s %n        to %s%n", mapping.source(), mapping.targetPath());
            throw ioe;
        }
    }

    private static String formatErrors(List<ExampleMapping> missingSources, List<ExampleMapping> invalidSources,
            List<ExampleMapping> invalidTargets, List<List<ExampleMapping>> duplicateTargets) {
        StringBuilder message = new StringBuilder("Example source copy failed:");
        if (!missingSources.isEmpty()) {
            message.append(System.lineSeparator()).append("Missing sources:");
            missingSources.stream()
                    .sorted(ExampleMapping.ORDER)
                    .forEach(mapping -> appendMapping(message, mapping, "- "));
        }
        if (!invalidSources.isEmpty()) {
            message.append(System.lineSeparator()).append("Invalid sources outside the project root:");
            invalidSources.stream()
                    .sorted(ExampleMapping.ORDER)
                    .forEach(mapping -> appendMapping(message, mapping, "- "));
        }
        if (!invalidTargets.isEmpty()) {
            message.append(System.lineSeparator()).append("Invalid targets outside the output directory:");
            invalidTargets.stream()
                    .sorted(ExampleMapping.ORDER)
                    .forEach(mapping -> appendMapping(message, mapping, "- "));
        }
        if (!duplicateTargets.isEmpty()) {
            message.append(System.lineSeparator()).append("Duplicate targets:");
            for (List<ExampleMapping> targetMappings : duplicateTargets) {
                message.append(System.lineSeparator()).append("- target: ").append(targetMappings.get(0).targetKey());
                message.append(System.lineSeparator()).append("  mappings:");
                targetMappings.stream()
                        .sorted(ExampleMapping.ORDER)
                        .forEach(mapping -> appendMapping(message, mapping, "  - "));
            }
        }
        return message.toString();
    }

    private static void appendMapping(StringBuilder message, ExampleMapping mapping, String prefix) {
        String detailsIndent = " ".repeat(prefix.length());
        message.append(System.lineSeparator()).append(prefix).append("manifest: ").append(mapping.manifestPath());
        message.append(System.lineSeparator()).append(detailsIndent).append("target: ").append(mapping.target());
        message.append(System.lineSeparator()).append(detailsIndent).append("source: ").append(mapping.source())
                .append(" (resolved: ").append(mapping.sourcePath()).append(')');
    }

    static class MappingList {
        List<Example> examples;
    }

    static class Example {
        String source;
        String target;
    }

    private record ExampleMapping(Path manifestPath, String source, Path sourcePath, boolean validSource, String target,
            String targetKey, Path targetPath, boolean validTarget) {
        private static final Comparator<ExampleMapping> ORDER = Comparator
                .comparing((ExampleMapping mapping) -> mapping.manifestPath().toString())
                .thenComparing(ExampleMapping::target)
                .thenComparing(ExampleMapping::source);
    }
}
