package io.quarkus.yaml.configuration.runtime;

import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.APPLICATION_YML_FILE;
import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.APPLICATION_YML_FILESYSTEM_PRIORITY;
import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.APPLICATION_YML_JAR_PRIORITY;
import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.MICROPROFILE_CONFIG_YML_FILE;
import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.MICROPROFILE_CONFIG_YML_FILESYSTEM_PRIORITY;
import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.MICROPROFILE_CONFIG_YML_JAR_PRIORITY;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class YamlConfigSourceProvider implements ConfigSourceProvider {

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        List<ConfigSource> configSources = new ArrayList<>();

        URL jarMicroprofileConfigYml = classLoader.getResource(MICROPROFILE_CONFIG_YML_FILE);
        if (jarMicroprofileConfigYml != null) {
            configSources.add(new YamlConfigSource(jarMicroprofileConfigYml, MICROPROFILE_CONFIG_YML_JAR_PRIORITY));
        }
        URL jarApplicationYml = classLoader.getResource(APPLICATION_YML_FILE);
        if (jarApplicationYml != null) {
            configSources.add(new YamlConfigSource(jarApplicationYml, APPLICATION_YML_JAR_PRIORITY));
        }

        URL fsMicroprofileConfigYml = getFileSystemURL(MICROPROFILE_CONFIG_YML_FILE);
        if (fsMicroprofileConfigYml != null) {
            configSources.add(new YamlConfigSource(fsMicroprofileConfigYml, MICROPROFILE_CONFIG_YML_FILESYSTEM_PRIORITY));
        }
        URL fsApplicationYml = getFileSystemURL(APPLICATION_YML_FILE);
        if (fsApplicationYml != null) {
            configSources.add(new YamlConfigSource(fsApplicationYml, APPLICATION_YML_FILESYSTEM_PRIORITY));
        }

        return configSources;
    }

    private URL getFileSystemURL(String source) {
        Path path = Paths.get("config", source);
        if (Files.exists(path)) {
            try {
                return path.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return null;
        }
    }
}
