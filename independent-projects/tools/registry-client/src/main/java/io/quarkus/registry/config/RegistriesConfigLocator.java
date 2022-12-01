package io.quarkus.registry.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * A helper class set utility methods to locate the registry client configuration file
 * in the default locations (e.g. user home <code>.quarkus</code> dir, or the project dir) or in
 * at the location specified by the caller.
 * Also includes methods to parse the registry client configuration file.
 */
public class RegistriesConfigLocator {

    public static final String CONFIG_RELATIVE_PATH = ".quarkus/config.yaml";
    public static final String CONFIG_FILE_PATH_PROPERTY = "quarkus.tools.config";

    static final String QUARKUS_REGISTRIES = "QUARKUS_REGISTRIES";
    static final String QUARKUS_REGISTRY_ENV_VAR_PREFIX = "QUARKUS_REGISTRY_";

    /**
     * Locate the registry client configuration file and deserialize it.
     *
     * The method will look for the file in the following locations in this order:
     * <ol>
     * <li>if <code>quarkus.config.root</code> system property is set, its value will be
     * used as the location of the configuration file</li>
     * <li>current user directory (which usually would be the project dir)</li>
     * <li><code>.quarkus/config.yaml</code> in the user home directory
     * </ol>
     *
     * If the configuration file can't be located (it is optional),
     * an empty configuration will be returned to the caller.
     *
     * @return registry client configuration, never null
     */
    public static RegistriesConfig resolveConfig() {
        final RegistriesConfig config = initFromEnvironmentOrNull(System.getenv());
        if (config != null) {
            return config;
        }
        final Path configYaml = locateConfigYaml();
        if (configYaml == null) {
            return new RegistriesConfigImpl.Builder().build().setSource(ConfigSource.DEFAULT);
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
            RegistriesConfigImpl.Builder config = RegistriesConfigMapperHelper.deserialize(configYaml,
                    RegistriesConfigImpl.Builder.class);
            if (config == null) { // empty file
                config = new RegistriesConfigImpl.Builder();
            }
            return config.setSource(new ConfigSource.FileConfigSource(configYaml)).build();
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
            RegistriesConfigImpl.Builder instance = RegistriesConfigMapperHelper.deserializeYaml(configYaml,
                    RegistriesConfigImpl.Builder.class);
            return instance == null ? null : instance.build();
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
            RegistriesConfigImpl.Builder instance = RegistriesConfigMapperHelper.deserializeYaml(configYaml,
                    RegistriesConfigImpl.Builder.class);
            return instance == null ? null : instance.build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse config file " + configYaml, e);
        }
    }

    /**
     * Returns the registry client configuration file or null, if the file could not be found.
     *
     * @return the registry client configuration file or null, if the file could not be found
     */
    public static Path locateConfigYaml() {
        return locateConfigYaml(null);
    }

    /**
     * Returns the registry client configuration file or null if the file could not be found.
     *
     * @param configYaml Path to a pre-specified config file (e.g. a command line argument)
     * @return the registry client configuration file or null if the file could not be found.
     */
    public static Path locateConfigYaml(Path configYaml) {
        if (configYaml == null) {
            final String prop = System.getProperty(CONFIG_FILE_PATH_PROPERTY);
            if (prop != null) {
                configYaml = Paths.get(prop);
                if (!Files.exists(configYaml)) {
                    throw new IllegalStateException("Quarkus extension registry configuration file " + configYaml
                            + " specified by the system property " + CONFIG_FILE_PATH_PROPERTY + " does not exist");
                }
                return configYaml;
            }

            configYaml = Paths.get("").normalize().toAbsolutePath().resolve(CONFIG_RELATIVE_PATH);
            if (Files.exists(configYaml)) {
                return configYaml;
            }

            configYaml = getDefaultConfigYamlLocation();
        }

        return Files.exists(configYaml) ? configYaml : null;
    }

    /**
     * Returns the default location of the registry client configuration file.
     *
     * @return the default location of the registry client configuration file
     */
    static Path getDefaultConfigYamlLocation() {
        return Paths.get(System.getProperty("user.home")).resolve(CONFIG_RELATIVE_PATH);
    }

    /**
     * Returns the default registry client configuration which should be used in case
     * no configuration file was found in the user's environment.
     *
     * @return default registry client configuration
     */
    public static RegistryConfig getDefaultRegistry() {
        return RegistryConfigImpl.getDefaultRegistry();
    }

    /**
     * @param map A Map containing environment variables, e.g. {@link System#getenv()}
     * @return A RegistriesConfig object initialized from environment variables.
     */
    static RegistriesConfig initFromEnvironmentOrNull(Map<String, String> map) {
        final String envRegistries = map.get(QUARKUS_REGISTRIES);
        if (envRegistries == null || envRegistries.isBlank()) {
            return null;
        }

        RegistriesConfigImpl.Builder registriesConfigBuilder = new RegistriesConfigImpl.Builder();

        for (String registryId : envRegistries.split(",")) {
            final RegistryConfigImpl.Builder builder = new RegistryConfigImpl.Builder()
                    .setId(registryId);

            final String envvarPrefix = getEnvVarPrefix(registryId);
            for (Map.Entry<String, String> var : map.entrySet()) {
                if (!var.getKey().startsWith(envvarPrefix)) {
                    continue;
                }
                if (isEnvVarOption(var.getKey(), envvarPrefix, "UPDATE_POLICY")) {
                    builder.setUpdatePolicy(var.getValue());

                } else if (isEnvVarOption(var.getKey(), envvarPrefix, "REPO_URL")) {
                    builder.setMaven(RegistryMavenConfig.builder()
                            .setRepository(RegistryMavenRepoConfig.builder()
                                    .setUrl(var.getValue())
                                    .build())
                            .build());
                }
            }

            registriesConfigBuilder.addRegistry(builder.build());
        }

        return registriesConfigBuilder
                .build()
                .setSource(ConfigSource.ENV);
    }

    private static boolean isEnvVarOption(String varName, String registryPrefix, String optionName) {
        return varName.regionMatches(registryPrefix.length(), optionName, 0, optionName.length());
    }

    private static String getEnvVarPrefix(String registryId) {
        final StringBuilder buf = new StringBuilder(QUARKUS_REGISTRY_ENV_VAR_PREFIX);
        for (int i = 0; i < registryId.length(); ++i) {
            final char c = registryId.charAt(i);
            if (c == '.') {
                buf.append('_');
            } else {
                buf.append(Character.toUpperCase(c));
            }
        }
        return buf.append('_').toString();
    }
}
