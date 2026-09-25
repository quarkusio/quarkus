package io.quarkus.gradle.application.internal.image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.quarkus.gradle.application.internal.ResultReceiptProperties;

public final class ImageReferenceResolutionCodec {

    private static final String SCHEMA_VERSION = "schema.version";
    private static final String PRIMARY_REFERENCE = "image.primary";
    private static final String ADDITIONAL_COUNT = "image.additional.count";
    private static final String ADDITIONAL_PREFIX = "image.additional.";

    public void write(Path file, ImageReferenceResolution resolution) {
        Path parent = file.toAbsolutePath().getParent();
        Path temporary = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            ResultReceiptProperties.store(toProperties(resolution), temporary);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write container image reference receipt " + file, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Preserve the original write failure, if any.
                }
            }
        }
    }

    public ImageReferenceResolution read(Path file) {
        rejectDuplicateKeys(file);
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read container image reference receipt " + file, e);
        }
        String schema = required(file, properties, SCHEMA_VERSION);
        if (!ImageReferenceResolution.SCHEMA_VERSION.equals(schema)) {
            throw new IllegalArgumentException("Unsupported schema version in " + file + ": " + schema);
        }
        int count = nonNegativeInt(file, required(file, properties, ADDITIONAL_COUNT), ADDITIONAL_COUNT);
        List<String> additional = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            additional.add(required(file, properties, ADDITIONAL_PREFIX + i));
        }
        for (String key : properties.stringPropertyNames()) {
            if (!SCHEMA_VERSION.equals(key) && !PRIMARY_REFERENCE.equals(key) && !ADDITIONAL_COUNT.equals(key)
                    && !isExpectedAdditionalKey(key, count)) {
                throw new IllegalArgumentException("Unknown field '" + key + "' in " + file);
            }
        }
        return new ImageReferenceResolution(required(file, properties, PRIMARY_REFERENCE), additional);
    }

    private static void rejectDuplicateKeys(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read container image reference receipt " + file, e);
        }
        java.util.HashSet<String> keys = new java.util.HashSet<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("Malformed field in " + file + ": " + line);
            }
            String key = line.substring(0, separator);
            if (!keys.add(key)) {
                throw new IllegalArgumentException("Duplicate field '" + key + "' in " + file);
            }
        }
    }

    private static Map<String, String> toProperties(ImageReferenceResolution resolution) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(SCHEMA_VERSION, ImageReferenceResolution.SCHEMA_VERSION);
        properties.put(PRIMARY_REFERENCE, resolution.primaryReference());
        properties.put(ADDITIONAL_COUNT, Integer.toString(resolution.additionalReferences().size()));
        for (int i = 0; i < resolution.additionalReferences().size(); i++) {
            properties.put(ADDITIONAL_PREFIX + i, resolution.additionalReferences().get(i));
        }
        return properties;
    }

    private static boolean isExpectedAdditionalKey(String key, int count) {
        if (!key.startsWith(ADDITIONAL_PREFIX)) {
            return false;
        }
        try {
            int index = Integer.parseInt(key.substring(ADDITIONAL_PREFIX.length()));
            return index >= 0 && index < count;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String required(Path file, Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field '" + key + "' in " + file);
        }
        return value;
    }

    private static int nonNegativeInt(Path file, String value, String key) {
        try {
            int result = Integer.parseInt(value);
            if (result >= 0) {
                return result;
            }
        } catch (NumberFormatException ignored) {
            // Report one consistent malformed-field error below.
        }
        throw new IllegalArgumentException("Invalid non-negative integer field '" + key + "' in " + file + ": " + value);
    }
}
