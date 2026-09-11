package io.quarkus.gradle.application.internal.nativeimage;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.gradle.application.internal.ResultReceiptProperties;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public final class NativeResultCodec {

    private static final String SCHEMA_VERSION = "schema.version";
    private static final String RESULT_TYPE = "result.type";
    private static final String BUILD_NAME = "build.name";
    private static final String OUTPUT_ROOT = "native.output-root";
    private static final String OUTPUT_NAME = "native.output-name";
    private static final String EXECUTABLE_PATH = "native.executable.path";
    private static final String SOURCES_DIRECTORY = "native.sources.directory";
    private static final String SOURCE_JAR_PATH = "native.source-jar.path";
    private static final String NATIVE_IMAGE_ARGS_PATH = "native.image.args.path";
    private static final String GRAALVM_PREFIX = "native.graalvm.";
    private static final String ARTIFACT_COUNT = "native.artifact.count";
    private static final String ARTIFACT_PREFIX = "native.artifact.";
    private static final String TYPE_SUFFIX = ".type";
    private static final String PATH_SUFFIX = ".path";
    private static final String METADATA_PREFIX = ".metadata.";

    public void write(Path file, NativeResult result) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ResultReceiptProperties.store(toProperties(file, result), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Quarkus native result receipt " + file, e);
        }
    }

    public NativeResult read(Path file) {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Quarkus native result receipt " + file, e);
        }
        String schemaVersion = required(file, properties, SCHEMA_VERSION);
        if (!NativeResult.SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported schema version in " + file + ": " + schemaVersion);
        }
        String resultType = required(file, properties, RESULT_TYPE);
        Path outputRoot = path(file, required(file, properties, OUTPUT_ROOT), file.getParent());
        int artifactCount = integer(file, properties.getProperty(ARTIFACT_COUNT, "0"), ARTIFACT_COUNT);
        var artifacts = new ArrayList<NativeResult.Artifact>(artifactCount);
        for (int i = 0; i < artifactCount; i++) {
            String prefix = ARTIFACT_PREFIX + i;
            artifacts.add(new NativeResult.Artifact(
                    optional(properties, prefix + PATH_SUFFIX).map(value -> path(file, value, outputRoot)),
                    required(file, properties, prefix + TYPE_SUFFIX),
                    metadata(properties, prefix + METADATA_PREFIX)));
        }
        return new NativeResult(
                required(file, properties, BUILD_NAME),
                buildType(file, resultType),
                outputRoot,
                required(file, properties, OUTPUT_NAME),
                optional(properties, EXECUTABLE_PATH).map(value -> path(file, value, outputRoot)),
                optional(properties, SOURCES_DIRECTORY).map(value -> path(file, value, outputRoot)),
                optional(properties, SOURCE_JAR_PATH).map(value -> path(file, value, outputRoot)),
                optional(properties, NATIVE_IMAGE_ARGS_PATH).map(value -> path(file, value, outputRoot)),
                metadata(properties, GRAALVM_PREFIX),
                artifacts);
    }

    private static Map<String, String> toProperties(Path file, NativeResult result) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(SCHEMA_VERSION, NativeResult.SCHEMA_VERSION);
        properties.put(RESULT_TYPE, result.resultType());
        properties.put(BUILD_NAME, result.buildName());
        properties.put(OUTPUT_ROOT, path(file.getParent(), result.outputRoot()));
        properties.put(OUTPUT_NAME, result.outputName());
        result.executablePath().ifPresent(value -> properties.put(EXECUTABLE_PATH, path(result.outputRoot(), value)));
        result.sourcesDirectory().ifPresent(value -> properties.put(SOURCES_DIRECTORY, path(result.outputRoot(), value)));
        result.sourceJarPath().ifPresent(value -> properties.put(SOURCE_JAR_PATH, path(result.outputRoot(), value)));
        result.nativeImageArgsPath()
                .ifPresent(value -> properties.put(NATIVE_IMAGE_ARGS_PATH, path(result.outputRoot(), value)));
        result.graalVMInfo().forEach((key, value) -> properties.put(GRAALVM_PREFIX + key, value));
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

    private static int integer(Path file, String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer field '" + key + "' in " + file + ": " + value, e);
        }
    }

    private static QuarkusApplicationBuildType buildType(Path file, String resultType) {
        return switch (resultType) {
            case "native-executable" -> QuarkusApplicationBuildType.NATIVE_EXECUTABLE;
            case "native-sources" -> QuarkusApplicationBuildType.NATIVE_SOURCES;
            default -> throw new IllegalArgumentException("Unsupported native result type in " + file + ": " + resultType);
        };
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
