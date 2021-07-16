package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistriesConfig implements RegistriesConfig {

    private boolean debug;
    private JsonRegistryConfig defaultRegistryConfig;

    @JsonSerialize(using = JsonRegistryConfigSerializer.class)
    @JsonDeserialize(using = JsonRegistryConfigDeserializer.class)
    private Map<String, RegistryConfig> registries = Collections.emptyMap();

    @Override
    @JsonIgnore
    public Collection<RegistryConfig> getRegistries() {
        return (Collection<RegistryConfig>) registries.values();
    }

    @JsonIgnore
    public Collection<RegistryConfig> getEnabledRegistries() {
        return registries.values().stream().filter(x -> !x.isDisabled()).collect(Collectors.toList());
    }

    public void setRegistryMap(Map<String, RegistryConfig> registries) {
        this.registries = registries == null ? Collections.emptyMap() : registries;
    }

    public void addRegistry(RegistryConfig registry) {
        if (registries.isEmpty()) {
            registries = new LinkedHashMap<>();
        }
        registries.put(registry.getId(), registry);
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return registries.isEmpty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(debug, registries);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistriesConfig other = (JsonRegistriesConfig) obj;
        return debug == other.debug && Objects.equals(registries, other.registries);
    }

    @Override
    public String toString() {
        return "JsonRegistriesConfig{" +
                "debug=" + debug +
                ", registries=" + registries +
                '}';
    }

    public RegistriesConfig completeRequiredConfig() {
        if (getEnabledRegistries().isEmpty()) {
            addRegistry(getDefaultRegistry());
        }
        return this;
    }

    /**
     * Returns the default registry client configuration which should be used in case
     * no configuration file was found in the user's environment.
     *
     * @return default registry client configuration
     */
    public static JsonRegistryConfig getDefaultRegistry() {
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
}
