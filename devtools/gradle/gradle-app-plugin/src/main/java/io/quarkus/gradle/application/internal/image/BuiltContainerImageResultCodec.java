package io.quarkus.gradle.application.internal.image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.gradle.application.internal.ResultReceiptProperties;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

public final class BuiltContainerImageResultCodec {

    private static final String SCHEMA_VERSION = "schema.version";
    private static final String RESULT_TYPE = "result.type";
    private static final String IMAGE_BUILDER = "image.builder";
    private static final String IMAGE_PUSHED = "image.pushed";
    private static final String IMAGE_REFERENCE = "image.reference";
    private static final String IMAGE_DIGEST = "image.digest";
    private static final String IMAGE_ID = "image.id";
    private static final String IMAGE_PULL_REQUIRED = "image.pull-required";
    private static final String IMAGE_WORKING_DIRECTORY = "image.working-directory";
    private static final String IMAGE_OUTPUT_DIRECTORY = "image.output-directory";

    public void write(Path file, BuiltContainerImage image) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ResultReceiptProperties.store(toProperties(image), file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write container image result receipt " + file, e);
        }
    }

    public BuiltContainerImage read(Path file) {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read container image result receipt " + file, e);
        }
        String schemaVersion = required(file, properties, SCHEMA_VERSION);
        if (!BuiltContainerImage.SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported schema version in " + file + ": " + schemaVersion);
        }
        return new BuiltContainerImage(
                required(file, properties, RESULT_TYPE),
                optional(properties, IMAGE_BUILDER).map(value -> builder(file, value)),
                bool(file, required(file, properties, IMAGE_PUSHED), IMAGE_PUSHED),
                optional(properties, IMAGE_REFERENCE),
                optional(properties, IMAGE_DIGEST),
                optional(properties, IMAGE_ID),
                optional(properties, IMAGE_PULL_REQUIRED).map(value -> bool(file, value, IMAGE_PULL_REQUIRED)),
                optional(properties, IMAGE_WORKING_DIRECTORY),
                optional(properties, IMAGE_OUTPUT_DIRECTORY));
    }

    private static Map<String, String> toProperties(BuiltContainerImage image) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(SCHEMA_VERSION, BuiltContainerImage.SCHEMA_VERSION);
        properties.put(RESULT_TYPE, image.resultType());
        image.builder().ifPresent(builder -> properties.put(IMAGE_BUILDER, builder.quarkusBuilderName()));
        properties.put(IMAGE_PUSHED, Boolean.toString(image.pushed()));
        image.reference().ifPresent(value -> properties.put(IMAGE_REFERENCE, value));
        image.digest().ifPresent(value -> properties.put(IMAGE_DIGEST, value));
        image.imageId().ifPresent(value -> properties.put(IMAGE_ID, value));
        image.pullRequired().ifPresent(value -> properties.put(IMAGE_PULL_REQUIRED, Boolean.toString(value)));
        image.workingDirectory().ifPresent(value -> properties.put(IMAGE_WORKING_DIRECTORY, value));
        image.outputDirectory().ifPresent(value -> properties.put(IMAGE_OUTPUT_DIRECTORY, value));
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

    private static QuarkusApplicationImageBuilder builder(Path file, String value) {
        for (QuarkusApplicationImageBuilder builder : QuarkusApplicationImageBuilder.values()) {
            if (builder.quarkusBuilderName().equals(value)) {
                return builder;
            }
        }
        throw new IllegalArgumentException("Unknown image builder in " + file + ": " + value);
    }
}
