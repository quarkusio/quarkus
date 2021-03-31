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

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        return mavenConfig == null;
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
        return "[" + id + " maven=" + mavenConfig + "]";
    }
}
