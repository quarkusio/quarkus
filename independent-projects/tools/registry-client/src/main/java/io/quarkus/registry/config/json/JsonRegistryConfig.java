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
import io.quarkus.registry.json.JsonBooleanTrueFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistryConfig implements RegistryConfig.Mutable {

    private String id;
    private boolean enabled = true;
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

    public Mutable setId(String id) {
        this.id = Objects.requireNonNull(id);
        return this;
    }

    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = JsonBooleanTrueFilter.class)
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Mutable setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public Mutable setUpdatePolicy(String updatePolicy) {
        this.updatePolicy = updatePolicy;
        return this;
    }

    @Override
    @JsonDeserialize(as = JsonRegistryDescriptorConfig.class)
    public RegistryDescriptorConfig getDescriptor() {
        return descriptor;
    }

    public Mutable setDescriptor(RegistryDescriptorConfig descriptor) {
        this.descriptor = descriptor;
        return this;
    }

    @JsonDeserialize(as = JsonRegistryPlatformsConfig.class)
    @Override
    public RegistryPlatformsConfig getPlatforms() {
        return platforms;
    }

    public Mutable setPlatforms(RegistryPlatformsConfig platforms) {
        this.platforms = platforms;
        return this;
    }

    @JsonDeserialize(as = JsonRegistryNonPlatformExtensionsConfig.class)
    @Override
    public RegistryNonPlatformExtensionsConfig getNonPlatformExtensions() {
        return nonPlatformExtensions;
    }

    public Mutable setNonPlatformExtensions(RegistryNonPlatformExtensionsConfig nonPlatformExtensions) {
        this.nonPlatformExtensions = nonPlatformExtensions;
        return this;
    }

    boolean isIdOnly() {
        return this.mavenConfig == null
                && this.enabled
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

    public Mutable setMaven(RegistryMavenConfig mavenConfig) {
        this.mavenConfig = mavenConfig;
        return this;
    }

    @JsonDeserialize(as = JsonRegistryQuarkusVersionsConfig.class)
    @Override
    public RegistryQuarkusVersionsConfig getQuarkusVersions() {
        return versionsConfig;
    }

    public Mutable setQuarkusVersions(RegistryQuarkusVersionsConfig quarkusVersions) {
        this.versionsConfig = quarkusVersions;
        return this;
    }

    @JsonAnyGetter
    @Override
    public Map<String, Object> getExtra() {
        return extra == null ? Collections.emptyMap() : extra;
    }

    public Mutable setExtra(Map<String, Object> extra) {
        this.extra = extra;
        return this;
    }

    @Override
    @JsonAnySetter
    public Mutable setExtra(String name, Object value) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        extra.put(name, value);
        return this;
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
        return Objects.hash(descriptor, enabled, extra, id, mavenConfig, nonPlatformExtensions, platforms,
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
        return Objects.equals(descriptor, other.descriptor) && enabled == other.enabled
                && Objects.equals(extra, other.extra) && Objects.equals(id, other.id)
                && Objects.equals(mavenConfig, other.mavenConfig)
                && Objects.equals(nonPlatformExtensions, other.nonPlatformExtensions)
                && Objects.equals(platforms, other.platforms) && Objects.equals(updatePolicy, other.updatePolicy)
                && Objects.equals(versionsConfig, other.versionsConfig);
    }

    @Override
    public RegistryConfig.Mutable mutable() {
        return new JsonRegistryConfig()
                .setExtra(this.extra)
                .setId(this.id)
                .setEnabled(this.enabled)
                .setUpdatePolicy(this.updatePolicy)
                .setDescriptor(this.descriptor)
                .setPlatforms(this.platforms)
                .setNonPlatformExtensions(this.nonPlatformExtensions)
                .setMaven(this.mavenConfig)
                .setQuarkusVersions(this.versionsConfig);
    }

    public JsonRegistryConfig build() {
        return this;
    }
}
