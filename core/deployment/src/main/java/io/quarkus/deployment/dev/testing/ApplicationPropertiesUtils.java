package io.quarkus.deployment.dev.testing;

import static org.eclipse.microprofile.config.spi.ConfigSource.CONFIG_ORDINAL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import io.quarkus.runtime.configuration.ConfigSourceOrdinal;

/**
 * Utilities to create temporary {@code application.properties} files used in testing.
 */
public final class ApplicationPropertiesUtils {
    public static final String APPLICATION_PROPERTIES = "application.properties";

    private ApplicationPropertiesUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an empty {@code application.properties} file in a new temporary directory.
     *
     * @param pathPrefix a {@code String} to be used in generating the temporary directory's name
     * @return the {@code Path} location where the {@code application.properties} file resides
     */
    public static Path createTempApplicationProperties(final String pathPrefix) {
        return createTempApplicationProperties(pathPrefix, Collections.emptyMap(), 0);
    }

    /**
     * Creates a {@code application.properties} file in a new temporary directory.
     *
     * @param pathPrefix a {@code String} to be used in generating the temporary directory's name
     * @param config a {@code Map} with the configuration content to store
     * @param ordinal an {@code ConfigSourceOrdinal} with the configuration ordinal
     * @return the {@code Path} location where the {@code application.properties} file resides
     */
    public static Path createTempApplicationProperties(
            final String pathPrefix,
            final Map<String, String> config,
            final ConfigSourceOrdinal ordinal) {
        return createTempApplicationProperties(pathPrefix, config, ordinal.getOrdinal());
    }

    /**
     * Creates a {@code application.properties} file in a new temporary directory.
     *
     * @param pathPrefix a {@code String} to be used in generating the temporary directory's name
     * @param config a {@code Map} with the configuration content to store
     * @param ordinal an {@code int} with the configuration ordinal
     * @return the {@code Path} location where the {@code application.properties} file resides
     */
    public static Path createTempApplicationProperties(
            final String pathPrefix,
            final Map<String, String> config,
            final int ordinal) {

        try {
            Path tempDirectory = Files.createTempDirectory(pathPrefix);
            Path propertiesFile = tempDirectory.resolve(APPLICATION_PROPERTIES);
            File file = Files.createFile(propertiesFile).toFile();

            file.deleteOnExit();
            tempDirectory.toFile().deleteOnExit();

            if (config != null && !config.isEmpty()) {
                Properties properties = new Properties();
                properties.put(CONFIG_ORDINAL, String.valueOf(ordinal));
                properties.putAll(config);
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    properties.store(outputStream, "");
                }
            }

            return tempDirectory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a configuration content in a temporary {@code application.properties} file.
     *
     * @param file an {@code URI} that points to the {@code application.properties} file.
     * @param config a {@code Map} with the configuration content to store
     * @param ordinal an {@code ConfigSourceOrdinal} with the configuration ordinal
     */
    public static void writeTempApplicationProperties(
            final URI file,
            final Map<String, String> config,
            final ConfigSourceOrdinal ordinal) {
        writeTempApplicationProperties(file, config, ordinal.getOrdinal());
    }

    /**
     * Writes a configuration content in a temporary {@code application.properties} file.
     *
     * @param file an {@code URI} that points to the {@code application.properties} file.
     * @param config a {@code Map} with the configuration content to store
     * @param ordinal an {@code int} with the configuration ordinal
     */
    public static void writeTempApplicationProperties(
            final URI file,
            final Map<String, String> config,
            final int ordinal) {

        try {
            Properties properties = new Properties();
            properties.put(CONFIG_ORDINAL, String.valueOf(ordinal));
            properties.putAll(config);
            try (FileOutputStream outputStream = new FileOutputStream(new File(file))) {
                properties.store(outputStream, "");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
