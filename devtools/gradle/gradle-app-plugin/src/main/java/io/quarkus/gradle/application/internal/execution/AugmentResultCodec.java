package io.quarkus.gradle.application.internal.execution;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.gradle.application.internal.ResultReceiptProperties;

public final class AugmentResultCodec {

    private static final String SCHEMA_VERSION = "schema.version";
    private static final String RESULT_COUNT = "result.count";
    private static final String RESULT_PREFIX = "result.";
    private static final String TYPE_SUFFIX = ".type";
    private static final String PATH_SUFFIX = ".path";
    private static final String METADATA_PREFIX = ".metadata.";
    private static final String JAR_PATH = "jar.path";
    private static final String JAR_ORIGINAL_ARTIFACT = "jar.original-artifact";
    private static final String JAR_LIBRARY_DIR = "jar.library-dir";
    private static final String JAR_MUTABLE = "jar.mutable";
    private static final String JAR_CLASSIFIER = "jar.classifier";
    private static final String NATIVE_RESULT = "native.result";
    private static final String GRAALVM_PREFIX = "graalvm.";
    private static final String VERSION = "1";

    public void write(Path file, AugmentResult result) {
        try {
            if (file.getParent() != null) {
                java.nio.file.Files.createDirectories(file.getParent());
            }
            ResultReceiptProperties.store(toProperties(result), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Quarkus augmentation result " + file, e);
        }
    }

    public List<ArtifactResult> readArtifactResults(Path file) {
        return read(file).getResults();
    }

    public AugmentResult read(Path file) {
        Properties properties = new Properties();
        try (var reader = java.nio.file.Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Quarkus augmentation result " + file, e);
        }
        String schemaVersion = properties.getProperty(SCHEMA_VERSION);
        if (!VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported Quarkus augmentation result schema in " + file + ": "
                    + schemaVersion);
        }
        int count = Integer.parseInt(properties.getProperty(RESULT_COUNT, "0"));
        List<ArtifactResult> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String prefix = RESULT_PREFIX + i;
            String path = properties.getProperty(prefix + PATH_SUFFIX);
            String type = properties.getProperty(prefix + TYPE_SUFFIX);
            results.add(new ArtifactResult(path == null || path.isBlank() ? null : Path.of(path), type,
                    metadata(properties, prefix + METADATA_PREFIX)));
        }
        return new AugmentResult(
                results,
                jar(properties),
                optionalPath(properties, NATIVE_RESULT),
                metadata(properties, GRAALVM_PREFIX));
    }

    private static Map<String, String> toProperties(AugmentResult result) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(SCHEMA_VERSION, VERSION);
        List<ArtifactResult> results = result.getResults() == null ? List.of() : result.getResults();
        properties.put(RESULT_COUNT, Integer.toString(results.size()));
        for (int i = 0; i < results.size(); i++) {
            ArtifactResult artifact = results.get(i);
            String prefix = RESULT_PREFIX + i;
            if (artifact.getPath() != null) {
                properties.put(prefix + PATH_SUFFIX, path(artifact.getPath()));
            }
            if (artifact.getType() != null) {
                properties.put(prefix + TYPE_SUFFIX, artifact.getType());
            }
            Map<String, String> metadata = artifact.getMetadata() == null ? Map.of() : artifact.getMetadata();
            metadata.forEach((key, value) -> properties.put(prefix + METADATA_PREFIX + key, value));
        }
        JarResult jar = result.getJar();
        if (jar != null) {
            if (jar.getPath() != null) {
                properties.put(JAR_PATH, path(jar.getPath()));
            }
            if (jar.getOriginalArtifact() != null) {
                properties.put(JAR_ORIGINAL_ARTIFACT, path(jar.getOriginalArtifact()));
            }
            if (jar.getLibraryDir() != null) {
                properties.put(JAR_LIBRARY_DIR, path(jar.getLibraryDir()));
            }
            properties.put(JAR_MUTABLE, Boolean.toString(jar.mutable()));
            if (jar.getClassifier() != null && !jar.getClassifier().isBlank()) {
                properties.put(JAR_CLASSIFIER, jar.getClassifier());
            }
        }
        if (result.getNativeResult() != null) {
            properties.put(NATIVE_RESULT, path(result.getNativeResult()));
        }
        Map<String, String> graalVMInfo = result.getGraalVMInfo() == null ? Map.of() : result.getGraalVMInfo();
        graalVMInfo.forEach((key, value) -> properties.put(GRAALVM_PREFIX + key, value));
        return properties;
    }

    private static JarResult jar(Properties properties) {
        Path path = optionalPath(properties, JAR_PATH);
        if (path == null) {
            return null;
        }
        return new JarResult(
                path,
                optionalPath(properties, JAR_ORIGINAL_ARTIFACT),
                optionalPath(properties, JAR_LIBRARY_DIR),
                Boolean.parseBoolean(properties.getProperty(JAR_MUTABLE, "false")),
                properties.getProperty(JAR_CLASSIFIER));
    }

    private static Path optionalPath(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value);
    }

    private static String path(Path path) {
        if (path.isAbsolute()) {
            return path.toString();
        }
        return path.toString().replace(File.separatorChar, '/');
    }

    private static Map<String, String> metadata(Properties properties, String prefix) {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                metadata.put(key.substring(prefix.length()), properties.getProperty(key));
            }
        }
        return metadata;
    }
}
