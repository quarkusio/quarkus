package io.quarkus.config.yaml.runtime;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.source.yaml.YamlConfigSource;

/**
 *
 */
public final class ApplicationYamlProvider implements ConfigSourceProvider {

    static final String APPLICATION_YAML = "application.yaml";

    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        List<ConfigSource> sources = Collections.emptyList();
        // mirror the in-JAR application.properties
        try {
            InputStream str = forClassLoader.getResourceAsStream(APPLICATION_YAML);
            if (str != null) {
                try (Closeable c = str) {
                    YamlConfigSource configSource = new YamlConfigSource(APPLICATION_YAML, str, 254);
                    assert sources.isEmpty();
                    sources = Collections.singletonList(configSource);
                }
            }
        } catch (IOException e) {
            // configuration problem should be thrown
            throw new IOError(e);
        }
        // mirror the on-filesystem application.properties
        final Path path = Paths.get("config", APPLICATION_YAML);
        if (Files.exists(path)) {
            try (InputStream str = Files.newInputStream(path)) {
                YamlConfigSource configSource = new YamlConfigSource(APPLICATION_YAML, str, 264);
                if (sources.isEmpty()) {
                    sources = Collections.singletonList(configSource);
                } else {
                    // todo: sources = List.of(sources.get(0), configSource);
                    sources = Arrays.asList(sources.get(0), configSource);
                }
            } catch (NoSuchFileException | FileNotFoundException e) {
                // skip (race)
            } catch (IOException e) {
                // configuration problem should be thrown
                throw new IOError(e);
            }
        }
        return sources;
    }
}
