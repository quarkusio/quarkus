package io.quarkus.docs.generation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Generate an asciidoc include file containing attributes representing the extension statuses.
 */
public class ExtensionStatusGenerator {

    // Tree map to keep the generated file more predictable
    private static Map<String, String> statuses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public static void main(String[] args) throws Exception {
        System.out.println("[INFO] Creating Extension statuses attributes generator: " + List.of(args));
        ExtensionStatusGenerator generator = new ExtensionStatusGenerator()
                .setTargetDir(Path.of(args[0]))
                .setExtensionRoots(
                        Arrays.stream(Arrays.copyOfRange(args, 1, args.length))
                                .map(Path::of)
                                .toList());

        System.out.println("[INFO] Reading extension statuses");
        generator.readStatuses();
        System.out.println("[INFO] Writing include file with extension statuses");
        generator.writeAsciidocFile();
        System.out.println("[INFO] Done");
    }

    Path targetDir;
    List<Path> extensionRoots;
    final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    public ExtensionStatusGenerator setTargetDir(Path targetDir) {
        this.targetDir = targetDir;
        return this;
    }

    public ExtensionStatusGenerator setExtensionRoots(List<Path> extensionRoots) {
        this.extensionRoots = extensionRoots;
        return this;
    }

    public void writeAsciidocFile() throws IOException {
        Files.createDirectories(targetDir);
        try (BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve("extension-statuses.adoc"),
                StandardOpenOption.CREATE)) {
            for (var entry : statuses.entrySet()) {
                // Asciidoc attribute format: ":key: value"
                writer.write(":" + entry.getKey().replace(":", "-") + "-extension-status: " + entry.getValue());
                writer.newLine();
            }
        }
    }

    public void readStatuses() throws IOException {
        if (extensionRoots == null || extensionRoots.isEmpty()) {
            return;
        }

        for (Path path : extensionRoots) {
            System.out.println("[INFO] Inspecting root: " + path);
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // We really want the yaml from target,
                    //  so let's try skipping sources and .cache directories to make things a bit faster:
                    String currentFileName = dir.getFileName().toString();
                    if (currentFileName.startsWith(".") || currentFileName.equals("src")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path extensionYaml, BasicFileAttributes attrs) {
                    String currentFileName = extensionYaml.getFileName().toString();
                    if (currentFileName.equals("quarkus-extension.yaml")) {
                        if (Files.exists(extensionYaml)) {
                            try (InputStream is = Files.newInputStream(extensionYaml)) {
                                Map<String, ?> map = yaml.load(is);
                                String artifact = Objects.toString(map.get("artifact"), null);
                                String artifactId;
                                String groupId;
                                if (artifact != null) {
                                    String[] split = artifact.split(":");
                                    if (split.length != 3) {
                                        System.err.println(
                                                "Artifact value is malformed. Expected the following format: groupId:artifactId:version ["
                                                        + extensionYaml.toAbsolutePath() + "]");
                                    }
                                    groupId = split[0];
                                    artifactId = split[1];

                                } else {
                                    groupId = Objects.toString(map.get("group-id"));
                                    artifactId = Objects.toString(map.get("artifact-id"));
                                }

                                String status = statusFromObject(((Map<String, ?>) map.get("metadata")).get("status"),
                                        extensionYaml);

                                if (status == null) {
                                    System.err.println(
                                            "Extension status is not defined [" + extensionYaml.toAbsolutePath() + "]");
                                } else {
                                    statuses.put((groupId + "-" + artifactId).replace('.', '-'), status);
                                }
                            } catch (IOException e) {
                                System.err.println(e.getMessage() + "[" + extensionYaml.toAbsolutePath() + "]");
                            }
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static final Set<String> availableStatuses = Set.of(
            "experimental", "preview", "stable", "deprecated");

    private static String statusFromObject(Object object, Path path) {
        if (object != null) {
            String value = object.toString().toLowerCase();
            if (availableStatuses.contains(value)) {
                return value;
            }
            System.err.println("Unknown status: " + value + "[" + path.toAbsolutePath() + "]");
        }
        return null;
    }
}
