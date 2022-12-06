package io.quarkus.docs.generation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

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
    Map<String, String> allTargets = new HashMap<>();

    // Two arguments: <target output directory> <root-source-directory> <dir containing -files.yaml>
    // ${project.basedir}/../target/asciidoc/generated/examples ${project.basedir}/.. ${project.basedir}/src/main/asciidoc
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
        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
                .setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        for (Path path : srcPaths) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (path.toString().endsWith("-examples.yaml")) {
                        System.out.println("[INFO] Reading: " + path);

                        MappingList mapping = om.readValue(path.toFile(), MappingList.class);

                        // For each example:
                        for (Example example : mapping.examples) {

                            // Resolve the source path against the root directory
                            Path relativePath = Path.of(example.source);
                            Path sourcePath = rootPath.resolve(relativePath);
                            // Resolve the target path against the output directory
                            Path targetPath = outputPath.resolve(example.target);

                            if (Files.exists(sourcePath)) {
                                // Record an error if the target already exists
                                String former = allTargets.put(example.target, example.source);
                                if (former != null) {
                                    System.err.printf(
                                            "[ERROR] Duplicate target: %s%n        Previous value: %s%n        New value: %s%n",
                                            example.target, former, example.source);

                                    mapping.duplicateKey = true;
                                    continue;
                                }

                                // Make sure required target directories exist
                                Files.createDirectories(targetPath.getParent());

                                // Copy the source file to the target file.
                                // Replace {{source}} in comment lines with the relative source path
                                try (BufferedReader br = new BufferedReader(
                                        new InputStreamReader(Files.newInputStream(sourcePath, StandardOpenOption.READ),
                                                "UTF-8"))) {
                                    try (PrintWriter bw = new PrintWriter(new OutputStreamWriter(
                                            Files.newOutputStream(targetPath, StandardOpenOption.CREATE,
                                                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)))) {
                                        String line;
                                        while ((line = br.readLine()) != null) {
                                            if (line.startsWith("// Source: {{source}}")) {
                                                bw.println("// Source: " + relativePath);
                                            } else if (line.startsWith("# Source: {{source}}")) {
                                                bw.println("# Source: " + relativePath);
                                            } else {
                                                bw.println(line);
                                            }
                                        }
                                        System.out.printf("[INFO] Copied %s %n        to %s%n", relativePath, targetPath);
                                    }
                                } catch (IOException ioe) {
                                    System.err.printf("[ERROR] Error copying %s %n        to %s%n", relativePath,
                                            targetPath);
                                    throw ioe;
                                }
                            } else {
                                System.err.println("[ERROR] Specified source file doesn't exist: " + sourcePath);
                                mapping.missingSource = true;
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    static class MappingList {
        List<Example> examples;

        @JsonIgnore
        boolean missingSource = false;

        @JsonIgnore
        boolean duplicateKey = false;
    }

    static class Example {
        String source;
        String target;
    }
}
