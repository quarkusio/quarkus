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
import java.util.Properties;

import io.smallrye.config.PropertiesConfigSource;

/**
 * A configuration source for {@code application.properties}.
 */
public abstract class ApplicationPropertiesConfigSource extends PropertiesConfigSource {
    private static final long serialVersionUID = -4694780118527396798L;

    static final String APPLICATION_PROPERTIES = "application.properties";
    static final String APPLICATION_PROPERTIES_PROFILE = "application-%s.properties";
    static final String MP_PROPERTIES = "META-INF/microprofile-config.properties";
    static final String MP_PROPERTIES_PROFILE = "META-INF/microprofile-config-%s.properties";

    ApplicationPropertiesConfigSource(InputStream is, int ordinal) {
        super(readProperties(is), APPLICATION_PROPERTIES, ordinal);
    }

    ApplicationPropertiesConfigSource(InputStream is, String nm, int ordinal) {
        super(readProperties(is), nm, ordinal);
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
            super(openStream(null), 250);
        }

        public InJar(String profile) {
            super(openStream(profile), 251);
        }

        private static InputStream openStream(String profile) {
            final String filename = profile == null ? APPLICATION_PROPERTIES
                    : String.format(APPLICATION_PROPERTIES_PROFILE, profile);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ApplicationPropertiesConfigSource.class.getClassLoader();
            }
            InputStream is;
            if (cl == null) {
                is = ClassLoader.getSystemResourceAsStream(filename);
            } else {
                is = cl.getResourceAsStream(filename);
            }
            return is;
        }
    }

    /**
     * Config that makes sure that the MP config in the application takes precedence over any other on the class path
     */
    public static final class MpConfigInJar extends ApplicationPropertiesConfigSource {
        public MpConfigInJar() {
            super(openStream(null), MP_PROPERTIES, 240);
        }

        public MpConfigInJar(String profile) {
            super(openStream(profile), MP_PROPERTIES, 241);
        }

        private static InputStream openStream(String profile) {
            final String filename = profile == null ? MP_PROPERTIES
                    : String.format(MP_PROPERTIES_PROFILE, profile);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ApplicationPropertiesConfigSource.class.getClassLoader();
            }
            InputStream is;
            if (cl == null) {
                is = ClassLoader.getSystemResourceAsStream(filename);
            } else {
                is = cl.getResourceAsStream(filename);
            }
            return is;
        }
    }

    public static final class InFileSystem extends ApplicationPropertiesConfigSource {
        public InFileSystem() {
            super(openStream(null), 260);
        }

        public InFileSystem(String profile) {
            super(openStream(profile), 261);
        }

        private static InputStream openStream(final String profile) {
            final Path path = Paths.get("config",
                    profile == null ? APPLICATION_PROPERTIES : String.format(APPLICATION_PROPERTIES_PROFILE, profile));
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
}
