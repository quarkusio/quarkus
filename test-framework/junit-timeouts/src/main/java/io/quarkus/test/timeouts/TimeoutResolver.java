package io.quarkus.test.timeouts;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Resolves the timeout of a test from {@code test-timeouts.properties}.
 * <p>
 * Keys all end in {@code .timeout} and are looked up from most to least specific:
 * <ol>
 * <li>{@code <module path>.<Class>.<method>.timeout}</li>
 * <li>{@code <module path>.<Class>.timeout}</li>
 * <li>{@code <module path>.timeout}, then the same for each parent path</li>
 * <li>{@code timeout}</li>
 * </ol>
 * The module path is relative to the directory holding the properties file, for example
 * {@code extensions/redis-client/runtime}, and {@code <Class>} is the class name without its package (nested classes
 * are separated with a dot).
 * <p>
 * The system property {@code quarkus.test.timeout} replaces the global {@code timeout}, and the system property
 * {@code quarkus.test.timeout.multiplier} scales whichever value is found.
 */
final class TimeoutResolver {

    static final String FILE_NAME = "test-timeouts.properties";
    static final String DEFAULT_PROPERTY = "quarkus.test.timeout";
    static final String MULTIPLIER_PROPERTY = "quarkus.test.timeout.multiplier";
    static final Duration FALLBACK = Duration.ofSeconds(20);

    private static final String SUFFIX = ".timeout";

    private final Properties properties;

    TimeoutResolver(Properties properties) {
        this.properties = properties;
    }

    /**
     * @param modulePath the path of the module relative to the repository root, using {@code /}, or null if unknown
     * @param className the class name without its package
     * @param methodName the test method, or null when resolving for the class
     */
    Duration resolve(String modulePath, String className, String methodName) {
        String value = null;
        if (modulePath != null) {
            if (methodName != null) {
                value = properties.getProperty(modulePath + "." + className + "." + methodName + SUFFIX);
            }
            if (value == null) {
                value = properties.getProperty(modulePath + "." + className + SUFFIX);
            }
            String path = modulePath;
            while (value == null && !path.isEmpty()) {
                value = properties.getProperty(path + SUFFIX);
                int slash = path.lastIndexOf('/');
                path = slash < 0 ? "" : path.substring(0, slash);
            }
        }
        return scale(value != null ? parse(value) : globalDefault());
    }

    private Duration globalDefault() {
        String value = System.getProperty(DEFAULT_PROPERTY);
        if (value == null) {
            value = properties.getProperty("timeout");
        }
        return value != null ? parse(value) : FALLBACK;
    }

    private static Duration scale(Duration duration) {
        String multiplier = System.getProperty(MULTIPLIER_PROPERTY);
        if (multiplier == null) {
            return duration;
        }
        return Duration.ofMillis(Math.round(duration.toMillis() * Double.parseDouble(multiplier)));
    }

    /**
     * Accepts {@code 500ms}, {@code 20s}, {@code 2m} or an ISO-8601 duration such as {@code PT20S}.
     */
    static Duration parse(String value) {
        String v = value.trim();
        try {
            if (v.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(v.substring(0, v.length() - 2).trim()));
            }
            if (v.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(v.substring(0, v.length() - 1).trim()));
            }
            if (v.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(v.substring(0, v.length() - 1).trim()));
            }
            return Duration.parse(v);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid test timeout '" + value + "', expected for example 500ms, 20s, 2m", e);
        }
    }

    /**
     * Resolver using the properties file found in the given directory or its closest parent, if any.
     */
    static TimeoutResolver load(Path from) {
        Properties properties = new Properties();
        Path file = findFile(from);
        if (file != null) {
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + file, e);
            }
        }
        return new TimeoutResolver(properties);
    }

    /**
     * @return the directory holding the properties file, or null
     */
    static Path findRoot(Path from) {
        Path file = findFile(from);
        return file == null ? null : file.getParent();
    }

    private static Path findFile(Path from) {
        for (Path dir = from; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Resolver for code running outside of a test, which can only see the global default.
     */
    static Duration global() {
        return scale(new TimeoutResolver(loadFromWorkingDirectory()).globalDefault());
    }

    private static Properties loadFromWorkingDirectory() {
        return load(Path.of("").toAbsolutePath()).properties;
    }
}
