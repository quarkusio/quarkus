package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryMavenConfig;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistryConfig implements RegistryConfig {

    private String id;
    private boolean disabled;
    private String updatePolicy;
    private RegistryDescriptorConfig descriptor;
    private RegistryPlatformsConfig platforms;
    private RegistryNonPlatformExtensionsConfig nonPlatformExtensions;
    private RegistryMavenConfig mavenConfig;
    private RegistryQuarkusVersionsConfig versionsConfig;
    private Map<String, Object> extra;

    public JsonRegistryConfig() {
    }

    public JsonRegistryConfig(String id) {
        setId(id);
    }

    @JsonIgnore
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Objects.requireNonNull(id, "Quarkus Extension Registry ID can't be null");
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public void setUpdatePolicy(String updatePolicy) {
        this.updatePolicy = updatePolicy;
    }

    @Override
    @JsonDeserialize(as = JsonRegistryDescriptorConfig.class)
    public RegistryDescriptorConfig getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(RegistryDescriptorConfig descriptor) {
        this.descriptor = descriptor;
    }

    @JsonDeserialize(as = JsonRegistryPlatformsConfig.class)
    @Override
    public RegistryPlatformsConfig getPlatforms() {
        return platforms;
    }

    public void setPlatforms(RegistryPlatformsConfig platforms) {
        this.platforms = platforms;
    }

    @JsonDeserialize(as = JsonRegistryNonPlatformExtensionsConfig.class)
    @Override
    public RegistryNonPlatformExtensionsConfig getNonPlatformExtensions() {
        return nonPlatformExtensions;
    }

    public void setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig nonPlatformExtensions) {
        this.nonPlatformExtensions = nonPlatformExtensions;
    }

    @JsonIgnore
    boolean isIdOnly() {
        return this.mavenConfig == null
                && !this.disabled
                && this.descriptor == null
                && this.nonPlatformExtensions == null
                && this.platforms == null
                && this.updatePolicy == null
                && this.versionsConfig == null
                && (this.extra == null || this.extra.isEmpty());
    }

    @JsonDeserialize(as = JsonRegistryMavenConfig.class)
    @Override
    public RegistryMavenConfig getMaven() {
        return mavenConfig;
    }

    public void setMaven(RegistryMavenConfig mavenConfig) {
        this.mavenConfig = mavenConfig;
    }

    @JsonDeserialize(as = JsonRegistryQuarkusVersionsConfig.class)
    @Override
    public RegistryQuarkusVersionsConfig getQuarkusVersions() {
        return versionsConfig;
    }

    public void setQuarkusVersions(RegistryQuarkusVersionsConfig quarkusVersions) {
        this.versionsConfig = quarkusVersions;
    }

    @JsonAnyGetter
    @Override
    public Map<String, Object> getExtra() {
        return extra == null ? Collections.emptyMap() : extra;
    }

    @JsonAnySetter
    public void setAny(String name, Object value) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        extra.put(name, value);
    }

    @Override
    public String toString() {
        return "JsonRegistryConfig{" +
                "id='" + id + '\'' +
                ", disabled=" + disabled +
                ", updatePolicy='" + updatePolicy + '\'' +
                ", descriptor=" + descriptor +
                ", platforms=" + platforms +
                ", nonPlatformExtensions=" + nonPlatformExtensions +
                ", mavenConfig=" + mavenConfig +
                ", versionsConfig=" + versionsConfig +
                ", extra=" + extra +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor, disabled, extra, id, mavenConfig, nonPlatformExtensions, platforms,
                updatePolicy, versionsConfig);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonRegistryConfig other = (JsonRegistryConfig) obj;
        return Objects.equals(descriptor, other.descriptor) && disabled == other.disabled
                && Objects.equals(id, other.id)
                && Objects.equals(mavenConfig, other.mavenConfig)
                && Objects.equals(nonPlatformExtensions, other.nonPlatformExtensions)
                && Objects.equals(platforms, other.platforms)
                && Objects.equals(updatePolicy, other.updatePolicy)
                && Objects.equals(versionsConfig, other.versionsConfig)
                && Objects.equals(extra, other.extra);
    }

    JsonRegistryConfig completeRequiredConfig(String id) {
        setId(id);

        //        if (descriptor != null && descriptor.getArtifact() != null) {
        //            return this;
        //        }
        //
        //        final String[] parts = id.split("\\.");
        //        final StringBuilder buf = new StringBuilder(id.length());
        //        int i = parts.length;
        //        buf.append(parts[--i]);
        //        while (--i >= 0) {
        //            buf.append('.').append(parts[i]);
        //        }
        //
        //        JsonRegistryDescriptorConfig dCfg = new JsonRegistryDescriptorConfig();
        //        dCfg.setArtifact(
        //                new ArtifactCoords(buf.toString(), Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null, Constants.JSON,
        //                        Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        //        setDescriptor(dCfg);
        return this;
    }

    private static URL toUrlOrNull(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
        }
        return null;
    }
}
