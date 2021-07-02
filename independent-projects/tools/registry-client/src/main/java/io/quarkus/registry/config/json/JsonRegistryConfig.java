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
        this.id = Objects.requireNonNull(id, "QER ID can't be null");
    }

    @JsonIgnore
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Objects.requireNonNull(id);
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

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    @JsonAnySetter
    public void setAny(String name, Object value) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        extra.put(name, value);
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(id);
        if (mavenConfig != null) {
            buf.append(" maven=").append(mavenConfig);
        }
        if (extra != null && !extra.isEmpty()) {
            buf.append(" extra=").append(extra);
        }
        return buf.append(']').toString();
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
                && Objects.equals(extra, other.extra) && Objects.equals(id, other.id)
                && Objects.equals(mavenConfig, other.mavenConfig)
                && Objects.equals(nonPlatformExtensions, other.nonPlatformExtensions)
                && Objects.equals(platforms, other.platforms) && Objects.equals(updatePolicy, other.updatePolicy)
                && Objects.equals(versionsConfig, other.versionsConfig);
    }
}
