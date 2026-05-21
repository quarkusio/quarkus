package io.quarkus.datasource.deployment.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Utility for loading default database versions from properties files.
 * <p>
 * Database-specific extensions (e.g., JDBC PostgreSQL, JDBC MySQL) use this to load
 * their default version from a Maven-filtered properties file that references
 * the version property from build-parent/pom.xml.
 */
public final class DatabaseVersionLoader {

    private DatabaseVersionLoader() {
    }

    /**
     * Load the default version for a database from a properties file.
     * <p>
     * The properties file must be named {@code <dbKind>-default-version.properties}
     * and contain a {@code default.version} property.
     *
     * @param dbKind the database kind (e.g., "postgresql", "mysql")
     * @return the default version string
     * @throws IllegalStateException if the properties file is missing or doesn't contain default.version
     * @throws UncheckedIOException if an I/O error occurs reading the properties file
     */
    public static String loadDefaultVersion(String dbKind) {
        String fileName = dbKind + "-default-version.properties";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalStateException("Cannot find " + fileName);
            }
            Properties props = new Properties();
            props.load(in);
            String version = props.getProperty("default.version");
            if (version == null) {
                throw new IllegalStateException("No default.version in " + fileName);
            }
            return version;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
