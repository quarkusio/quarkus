package io.quarkus.registry.config;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryDescriptorConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig;
import io.quarkus.registry.config.json.JsonRegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

/**
 * A helper class with utility methods to locate the registry client configuration file
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
            return completeRequiredConfig(new JsonRegistriesConfig());
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
            return completeRequiredConfig(RegistriesConfigMapperHelper.deserialize(configYaml, JsonRegistriesConfig.class));
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
            return completeRequiredConfig(RegistriesConfigMapperHelper.deserializeYaml(configYaml, JsonRegistriesConfig.class));
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
            return completeRequiredConfig(RegistriesConfigMapperHelper.deserializeYaml(configYaml, JsonRegistriesConfig.class));
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
        configYaml = getDefaultConfigYamlLocation();
        return Files.exists(configYaml) ? configYaml : null;
    }

    /**
     * Returns the default location of the registry client configuration file.
     *
     * @return the default location of the registry client configuration file
     */
    public static Path getDefaultConfigYamlLocation() {
        return Paths.get(PropertiesUtil.getProperty("user.home")).resolve(CONFIG_RELATIVE_PATH);
    }

    static RegistriesConfig completeRequiredConfig(RegistriesConfig original) {
        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        config.setDebug(original.isDebug());
        if (original.getRegistries().isEmpty()) {
            config.addRegistry(getDefaultRegistry());
        } else {
            boolean sawEnabled = false;
            for (RegistryConfig qerConfig : original.getRegistries()) {
                config.addRegistry(completeRequiredConfig(qerConfig));
                sawEnabled |= qerConfig.isEnabled();
            }
            if (!sawEnabled) {
                config.addRegistry(getDefaultRegistry());
            }
        }
        return config;
    }

    private static RegistryConfig completeRequiredConfig(RegistryConfig original) {
        if (hasRequiredConfig(original)) {
            return original;
        }
        final String id = original.getId();
        final JsonRegistryConfig config = new JsonRegistryConfig(id);
        config.setUpdatePolicy(original.getUpdatePolicy());
        config.setEnabled(original.isEnabled());
        config.setDescriptor(completeDescriptor(original));
        if (original != null) {
            if (original.getMaven() != null) {
                config.setMaven(original.getMaven());
            }
            if (original.getNonPlatformExtensions() != null) {
                config.setNonPlatformExtensions(original.getNonPlatformExtensions());
            }
            if (original.getPlatforms() != null) {
                config.setPlatforms(original.getPlatforms());
            }
            if (!original.getExtra().isEmpty()) {
                config.setExtra(original.getExtra());
            }
        }
        return config;
    }

    private static RegistryDescriptorConfig completeDescriptor(RegistryConfig config) {
        if (config.getDescriptor() != null && config.getDescriptor().getArtifact() != null) {
            return config.getDescriptor();
        }
        final JsonRegistryDescriptorConfig descriptor = new JsonRegistryDescriptorConfig();
        String host = config.getId();
        if (host == null) {
            final RegistryMavenRepoConfig repo = config.getMaven() == null ? null : config.getMaven().getRepository();
            if (repo != null && repo.getUrl() != null) {
                throw new IllegalStateException(
                        "Failed to determine the descriptor coordinates for a registry with no ID and no Maven configuration");
            }
            host = Objects.requireNonNull(toUrlOrNull(repo.getUrl()), "REST endpoint is not a valid URL").getHost();
        }
        final String[] parts = host.split("\\.");
        final StringBuilder buf = new StringBuilder(host.length());
        int i = parts.length;
        buf.append(parts[--i]);
        while (--i >= 0) {
            buf.append('.').append(parts[i]);
        }
        descriptor.setArtifact(
                new ArtifactCoords(buf.toString(), Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null, Constants.JSON,
                        Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        return descriptor;
    }

    private static URL toUrlOrNull(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
        }
        return null;
    }

    private static boolean hasRequiredConfig(RegistryConfig qerConfig) {
        if (qerConfig.getId() == null) {
            return false;
        }
        if (qerConfig.getDescriptor() == null) {
            return false;
        }
        return true;
    }

    /**
     * Returns the default registry client configuration which should be used in case
     * no configuration file was found in the user's environment.
     *
     * @return default registry client configuration
     */
    public static RegistryConfig getDefaultRegistry() {
        final JsonRegistryConfig qer = new JsonRegistryConfig();
        qer.setId(Constants.DEFAULT_REGISTRY_ID);

        final JsonRegistryDescriptorConfig descriptor = new JsonRegistryDescriptorConfig();
        qer.setDescriptor(descriptor);
        descriptor.setArtifact(
                new ArtifactCoords(Constants.DEFAULT_REGISTRY_GROUP_ID, Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                        Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));

        final JsonRegistryMavenConfig mavenConfig = new JsonRegistryMavenConfig();
        qer.setMaven(mavenConfig);

        final JsonRegistryPlatformsConfig platformsConfig = new JsonRegistryPlatformsConfig();
        qer.setPlatforms(platformsConfig);
        platformsConfig.setArtifact(new ArtifactCoords(Constants.DEFAULT_REGISTRY_GROUP_ID,
                Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));

        final JsonRegistryNonPlatformExtensionsConfig nonPlatformExtensionsConfig = new JsonRegistryNonPlatformExtensionsConfig();
        qer.setNonPlatformExtensions(nonPlatformExtensionsConfig);
        nonPlatformExtensionsConfig.setArtifact(new ArtifactCoords(Constants.DEFAULT_REGISTRY_GROUP_ID,
                Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));

        final JsonRegistryMavenRepoConfig mavenRepo = new JsonRegistryMavenRepoConfig();
        mavenConfig.setRepository(mavenRepo);
        mavenRepo.setId(Constants.DEFAULT_REGISTRY_ID);
        mavenRepo.setUrl(Constants.DEFAULT_REGISTRY_MAVEN_REPO_URL);
        return qer;
    }

    static RegistriesConfig initFromEnvironmentOrNull(Map<String, String> map) {
        final String envRegistries = map.get(QUARKUS_REGISTRIES);
        if (envRegistries == null || envRegistries.isBlank()) {
            return null;
        }
        final JsonRegistriesConfig registries = new JsonRegistriesConfig();
        for (String registryId : envRegistries.split(",")) {
            final JsonRegistryConfig registry = new JsonRegistryConfig();
            registry.setId(registryId);
            registries.addRegistry(registry);

            final String envvarPrefix = getEnvVarPrefix(registryId);
            for (Map.Entry<String, String> var : map.entrySet()) {
                if (!var.getKey().startsWith(envvarPrefix)) {
                    continue;
                }

                if (isEnvVarOption(var.getKey(), envvarPrefix, "UPDATE_POLICY")) {
                    registry.setUpdatePolicy(var.getValue());
                } else if (isEnvVarOption(var.getKey(), envvarPrefix, "REPO_URL")) {
                    JsonRegistryMavenConfig maven = (JsonRegistryMavenConfig) registry.getMaven();
                    if (maven == null) {
                        maven = new JsonRegistryMavenConfig();
                        registry.setMaven(maven);
                    }
                    JsonRegistryMavenRepoConfig repository = (JsonRegistryMavenRepoConfig) maven.getRepository();
                    if (repository == null) {
                        repository = new JsonRegistryMavenRepoConfig();
                        maven.setRepository(repository);
                    }
                    repository.setUrl(var.getValue());
                }
            }
        }
        return completeRequiredConfig(registries);
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
