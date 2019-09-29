package io.quarkus.runtime.configuration;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.jboss.logging.Logger;

import io.smallrye.config.PropertiesConfigSource;

/**
 * A configuration source for {@code application.properties}.
 */
public abstract class ApplicationPropertiesConfigSource extends PropertiesConfigSource {
    private static final long serialVersionUID = -4694780118527396798L;
    static final String APPLICATION_PROPERTIES = "application.properties";

    ApplicationPropertiesConfigSource(InputStream is, int ordinal) {
        super(readProperties(is), APPLICATION_PROPERTIES, ordinal);
    }

    private static Map<String, String> readProperties(final InputStream is) {
        if (is == null) {
            return Collections.emptyMap();
        }
        try (Closeable ignored = is) {
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    final Properties properties = new Properties();
                    properties.load(br);
                    return (Map<String, String>) (Map) properties;
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static final class InJar extends ApplicationPropertiesConfigSource {
        public InJar() {
            super(openStream(), 250);
        }

        private static InputStream openStream() {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ApplicationPropertiesConfigSource.class.getClassLoader();
            }
            InputStream is;
            if (cl == null) {
                is = ClassLoader.getSystemResourceAsStream(APPLICATION_PROPERTIES);
            } else {
                is = cl.getResourceAsStream(APPLICATION_PROPERTIES);
            }
            return is;
        }
    }

    public static final class InFileSystem extends ApplicationPropertiesConfigSource {
        public InFileSystem() {
            super(openStream(), 260);
        }

        private static InputStream openStream() {
            final Path path = Paths.get("config", APPLICATION_PROPERTIES);
            if (Files.exists(path)) {
                try {
                    return Files.newInputStream(path);
                } catch (NoSuchFileException | FileNotFoundException e) {
                    return null;
                } catch (IOException e) {
                    throw new IOError(e);
                }
            } else {
                return null;
            }
        }
    }

    public static final class LocalConfigFile extends ApplicationPropertiesConfigSource {
        private static final Logger LOGGER = Logger.getLogger(LocalConfigFile.class.getName());

        public LocalConfigFile() {
            super(openStream(), 290);
        }

        private static InputStream openStream() {
            final Optional<String> localConfigFile = getLocalConfigFilename();
            if (localConfigFile.isPresent()) {

                final Path localConfigFilePath = Paths.get(localConfigFile.get());

                if (Files.notExists(localConfigFilePath)) {
                    LOGGER.warnf("Supplied local configuration file %s does not exist.", localConfigFilePath);
                    return null;
                }

                try {
                    return Files.newInputStream(localConfigFilePath);
                } catch (NoSuchFileException | FileNotFoundException e) {
                    return null;
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }

            return null;
        }

        public static Optional<String> getLocalConfigFilename() {
            final String envFile = System.getProperty("quarkus.local-config");
            if (envFile == null) {
                return Optional.empty();
            }

            return Optional.of(envFile);
        }
    }
}
