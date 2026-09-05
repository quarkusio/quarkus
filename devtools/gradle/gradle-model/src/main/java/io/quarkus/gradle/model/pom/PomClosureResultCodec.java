package io.quarkus.gradle.model.pom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.maven.dependency.GAV;

public final class PomClosureResultCodec {

    private static final String COUNT = "count";
    private static final String PREFIX = "entry.";
    private static final String GAV = ".gav";
    private static final String RESOLVED = ".resolved";
    private static final String FILE = ".file";

    private PomClosureResultCodec() {
    }

    public static void write(PomClosureResult result, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        PropertyUtils.store(toProperties(result), file);
    }

    public static PomClosureResult read(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        int count = requiredInt(properties, COUNT);
        Map<GAV, java.io.File> resolved = new TreeMap<>(PomClosureResultCodec::compare);
        Set<GAV> missing = new TreeSet<>(PomClosureResultCodec::compare);
        for (int i = 0; i < count; i++) {
            String prefix = PREFIX + i;
            GAV gav = parseGav(required(properties, prefix + GAV));
            boolean resolvedEntry = Boolean.parseBoolean(required(properties, prefix + RESOLVED));
            if (resolvedEntry) {
                resolved.put(gav, Path.of(required(properties, prefix + FILE)).toFile());
            } else {
                missing.add(gav);
            }
        }
        return new PomClosureResult(resolved, missing);
    }

    private static Properties toProperties(PomClosureResult result) {
        Properties properties = new Properties();
        int index = 0;
        Map<GAV, java.io.File> resolved = new TreeMap<>(PomClosureResultCodec::compare);
        resolved.putAll(result.resolvedPoms());
        for (Map.Entry<GAV, java.io.File> entry : resolved.entrySet()) {
            writeEntry(properties, index++, entry.getKey(), true, entry.getValue());
        }
        Set<GAV> missing = new TreeSet<>(PomClosureResultCodec::compare);
        missing.addAll(result.missingPoms());
        for (GAV gav : missing) {
            writeEntry(properties, index++, gav, false, null);
        }
        properties.setProperty(COUNT, Integer.toString(index));
        return properties;
    }

    private static void writeEntry(Properties properties, int index, GAV gav, boolean resolved, java.io.File file) {
        String prefix = PREFIX + index;
        properties.setProperty(prefix + GAV, gav.toString());
        properties.setProperty(prefix + RESOLVED, Boolean.toString(resolved));
        if (resolved) {
            properties.setProperty(prefix + FILE, file.getAbsolutePath());
        }
    }

    private static int requiredInt(Properties properties, String key) {
        try {
            return Integer.parseInt(required(properties, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("POM closure property '" + key + "' must be an integer", e);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("POM closure file is missing property '" + key + "'");
        }
        return value;
    }

    private static GAV parseGav(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("POM closure GAV must have format groupId:artifactId:version: " + value);
        }
        return new GAV(parts[0], parts[1], parts[2]);
    }

    private static int compare(GAV left, GAV right) {
        return left.toString().compareTo(right.toString());
    }
}
