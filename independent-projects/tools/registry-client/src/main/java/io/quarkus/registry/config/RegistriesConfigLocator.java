package io.quarkus.registry.config;

import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A helper class with utility methods to locate the registry client configuration file
 * in the default locations (e.g. user home <code>.quarkus</code> dir, or the project dir) or in
 * at the location specified by the caller.
 * Also includes methods to parse the registry client configuration file.
 */
public class RegistriesConfigLocator {

    public static final String CONFIG_RELATIVE_PATH = ".quarkus/config.yaml";
    public static final String CONFIG_FILE_PATH_PROPERTY = "qer.config";

    /**
     * Locate the registry client configuration file and deserialize it.
     * The method will be looking for the file in the following locations in this order:
     * <ol>
     * <li>if <code>qer.config</code> system property is set, its value will be used as the location of the configuration
     * file</li>
     * <li>current user directory (which usually would be the project dir)</li>
     * <li><code>.quarkus/config.yaml</code> in the user home directory
     * </ol>
     *
     * Given that the presence of the configuration file is optional, if the configuration file couldn't be located,
     * an empty configuration would be returned to the caller.
     *
     * @return registry client configuration, never null
     */
    public static RegistriesConfig resolveConfig() {
        final Path configYaml = locateConfigYaml();
        if (configYaml == null) {
            return new JsonRegistriesConfig().completeRequiredConfig();
        }
        return load(configYaml);
    }

    /**
     * Deserializes a given configuration file.
     *
     * @param configYaml configuration file
     * @return deserialized registry client configuration
     */
    public static RegistriesConfig load(Path configYaml) {
        try {
            return RegistriesConfigMapperHelper.deserialize(configYaml, JsonRegistriesConfig.class).completeRequiredConfig();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse config file " + configYaml, e);
        }
    }

    /**
     * Deserializes registry client configuration from an input stream.
     *
     * @param configYaml input stream
     * @return deserialized registry client configuration
     */
    public static RegistriesConfig load(InputStream configYaml) {
        try {
            return RegistriesConfigMapperHelper.deserializeYaml(configYaml, JsonRegistriesConfig.class)
                    .completeRequiredConfig();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse config file " + configYaml, e);
        }
    }

    /**
     * Deserializes registry client configuration from a reader.
     *
     * @param configYaml reader
     * @return deserialized registry client configuration
     */
    public static RegistriesConfig load(Reader configYaml) {
        try {
            return RegistriesConfigMapperHelper.deserializeYaml(configYaml, JsonRegistriesConfig.class)
                    .completeRequiredConfig();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse config file " + configYaml, e);
        }
    }

    public static Path locateConfigYaml() {
        final String prop = PropertiesUtil.getProperty(CONFIG_FILE_PATH_PROPERTY);
        Path configYaml;
        if (prop != null) {
            configYaml = Paths.get(prop);
            if (!Files.exists(configYaml)) {
                throw new IllegalStateException("Quarkus extensions registry configuration file " + configYaml
                        + " specified with the system property " + CONFIG_FILE_PATH_PROPERTY + " does not exist");
            }
            return configYaml;
        }
        configYaml = Paths.get("").normalize().toAbsolutePath().resolve(CONFIG_RELATIVE_PATH);
        if (Files.exists(configYaml)) {
            return configYaml;
        }
        configYaml = Paths.get(PropertiesUtil.getProperty("user.home")).resolve(CONFIG_RELATIVE_PATH);
        return Files.exists(configYaml) ? configYaml : null;
    }

}
