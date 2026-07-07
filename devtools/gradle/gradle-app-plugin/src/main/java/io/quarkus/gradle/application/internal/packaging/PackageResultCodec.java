package io.quarkus.gradle.application.internal.packaging;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.gradle.application.internal.ResultReceiptProperties;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public final class PackageResultCodec {

    private static final String SCHEMA_VERSION = "schema.version";
    private static final String RESULT_TYPE = "result.type";
    private static final String RESULT_TYPE_VALUE = "jvm-package";
    private static final String BUILD_NAME = "build.name";
    private static final String PACKAGE_TYPE = "package.type";
    private static final String OUTPUT_ROOT = "package.output-root";
    private static final String OUTPUT_NAME = "package.output-name";
    private static final String JAR_PATH = "package.jar.path";
    private static final String ORIGINAL_ARTIFACT = "package.original-artifact";
    private static final String LIBRARY_DIR = "package.library-dir";
    private static final String MUTABLE = "package.mutable";
    private static final String UBER = "package.uber";
    private static final String CLASSIFIER = "package.classifier";
    private static final String ARTIFACT_COUNT = "package.artifact.count";
    private static final String ARTIFACT_PREFIX = "package.artifact.";
    private static final String TYPE_SUFFIX = ".type";
    private static final String PATH_SUFFIX = ".path";
    private static final String METADATA_PREFIX = ".metadata.";

    public void write(Path file, PackageResult result) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ResultReceiptProperties.store(toProperties(file, result), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Quarkus package result receipt " + file, e);
        }
    }

    public PackageResult read(Path file) {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Quarkus package result receipt " + file, e);
        }
        String schemaVersion = required(file, properties, SCHEMA_VERSION);
        if (!PackageResult.SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported schema version in " + file + ": " + schemaVersion);
        }
        String resultType = required(file, properties, RESULT_TYPE);
        if (!RESULT_TYPE_VALUE.equals(resultType)) {
            throw new IllegalArgumentException("Unsupported package result type in " + file + ": " + resultType);
        }
        Path outputRoot = path(file, required(file, properties, OUTPUT_ROOT), file.getParent());
        int artifactCount = integer(file, properties.getProperty(ARTIFACT_COUNT, "0"), ARTIFACT_COUNT);
        var artifacts = new java.util.ArrayList<PackageResult.Artifact>(artifactCount);
        for (int i = 0; i < artifactCount; i++) {
            String prefix = ARTIFACT_PREFIX + i;
            artifacts.add(new PackageResult.Artifact(
                    optional(properties, prefix + PATH_SUFFIX).map(value -> path(file, value, outputRoot)),
                    required(file, properties, prefix + TYPE_SUFFIX),
                    metadata(properties, prefix + METADATA_PREFIX)));
        }
        return new PackageResult(
                required(file, properties, BUILD_NAME),
                buildType(file, required(file, properties, PACKAGE_TYPE)),
                outputRoot,
                required(file, properties, OUTPUT_NAME),
                path(file, required(file, properties, JAR_PATH), outputRoot),
                optional(properties, ORIGINAL_ARTIFACT).map(value -> path(file, value, outputRoot)),
                optional(properties, LIBRARY_DIR).map(value -> path(file, value, outputRoot)),
                bool(file, required(file, properties, MUTABLE), MUTABLE),
                bool(file, required(file, properties, UBER), UBER),
                optional(properties, CLASSIFIER),
                artifacts);
    }

    private static Map<String, String> toProperties(Path file, PackageResult result) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(SCHEMA_VERSION, PackageResult.SCHEMA_VERSION);
        properties.put(RESULT_TYPE, RESULT_TYPE_VALUE);
        properties.put(BUILD_NAME, result.buildName());
        properties.put(PACKAGE_TYPE, result.buildType().jarType().orElse(result.buildType().name()));
        properties.put(OUTPUT_ROOT, path(file.getParent(), result.outputRoot()));
        properties.put(OUTPUT_NAME, result.outputName());
        properties.put(JAR_PATH, path(result.outputRoot(), result.jarPath()));
        result.originalArtifact().ifPresent(value -> properties.put(ORIGINAL_ARTIFACT, path(result.outputRoot(), value)));
        result.libraryDirectory().ifPresent(value -> properties.put(LIBRARY_DIR, path(result.outputRoot(), value)));
        properties.put(MUTABLE, Boolean.toString(result.mutable()));
        properties.put(UBER, Boolean.toString(result.uberJar()));
        result.classifier().ifPresent(value -> properties.put(CLASSIFIER, value));
        properties.put(ARTIFACT_COUNT, Integer.toString(result.artifacts().size()));
        for (int i = 0; i < result.artifacts().size(); i++) {
            var artifact = result.artifacts().get(i);
            String prefix = ARTIFACT_PREFIX + i;
            artifact.path().ifPresent(value -> properties.put(prefix + PATH_SUFFIX, path(result.outputRoot(), value)));
            properties.put(prefix + TYPE_SUFFIX, artifact.type());
            artifact.metadata().forEach((key, value) -> properties.put(prefix + METADATA_PREFIX + key, value));
        }
        return properties;
    }

    private static String required(Path file, Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field '" + key + "' in " + file);
        }
        return value;
    }

    private static Optional<String> optional(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key)).filter(value -> !value.isBlank());
    }

    private static boolean bool(Path file, String value, String key) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean field '" + key + "' in " + file + ": " + value);
    }

    private static int integer(Path file, String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer field '" + key + "' in " + file + ": " + value, e);
        }
    }

    private static QuarkusApplicationBuildType buildType(Path file, String value) {
        for (QuarkusApplicationBuildType type : QuarkusApplicationBuildType.values()) {
            if (type.jarType().filter(value::equals).isPresent()) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown package type in " + file + ": " + value);
    }

    private static Path path(Path file, String value, Path base) {
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            return path;
        }
        if (base == null) {
            throw new IllegalArgumentException("Relative path field in " + file + " has no base directory: " + value);
        }
        return base.resolve(path).normalize();
    }

    private static String path(Path base, Path path) {
        if (base != null && path.isAbsolute() == base.isAbsolute()) {
            Path relative = base.normalize().relativize(path.normalize());
            return relative.toString().isEmpty() ? "." : relative.toString().replace(File.separatorChar, '/');
        }
        return path.isAbsolute() ? path.toString() : path.toString().replace(File.separatorChar, '/');
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
