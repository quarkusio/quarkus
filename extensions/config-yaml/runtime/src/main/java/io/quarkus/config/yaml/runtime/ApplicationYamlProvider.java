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
import java.util.ArrayList;
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

    public static final String APPLICATION_YAML = "application.yaml";
    private static final int APPLICATION_YAML_IN_JAR_ORDINAL = 254;
    public static final String APPLICATION_YML = "application.yml";
    private static final int APPLICATION_YML_IN_JAR_ORDINAL = 253;

    @Override
    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
        List<ConfigSource> yamlSources = getConfigSourcesForFileName(APPLICATION_YAML, APPLICATION_YAML_IN_JAR_ORDINAL,
                forClassLoader);
        List<ConfigSource> ymlSources = getConfigSourcesForFileName(APPLICATION_YML, APPLICATION_YML_IN_JAR_ORDINAL,
                forClassLoader);
        if (yamlSources.isEmpty() && ymlSources.isEmpty()) {
            return Collections.emptyList();
        } else if (yamlSources.isEmpty()) {
            return ymlSources;
        } else if (ymlSources.isEmpty()) {
            return yamlSources;
        }
        List<ConfigSource> result = new ArrayList<>(yamlSources.size() + ymlSources.size());
        result.addAll(yamlSources);
        result.addAll(ymlSources);
        return result;
    }

    private List<ConfigSource> getConfigSourcesForFileName(String fileName, int inJarOrdinal, ClassLoader forClassLoader) {
        List<ConfigSource> sources = Collections.emptyList();
        // mirror the in-JAR application.properties
        try {
            InputStream str = forClassLoader.getResourceAsStream(fileName);
            if (str != null) {
                try (Closeable c = str) {
                    YamlConfigSource configSource = new YamlConfigSource(fileName, str, inJarOrdinal);
                    assert sources.isEmpty();
                    sources = Collections.singletonList(configSource);
                }
            }
        } catch (IOException e) {
            // configuration problem should be thrown
            throw new IOError(e);
        }
        // mirror the on-filesystem application.properties
        final Path path = Paths.get("config", fileName);
        if (Files.exists(path)) {
            try (InputStream str = Files.newInputStream(path)) {
                YamlConfigSource configSource = new YamlConfigSource(fileName, str, inJarOrdinal + 10);
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
