package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.registry.config.ConfigSource;
import io.quarkus.registry.config.MutableRegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Deprecated
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistriesConfig implements MutableRegistriesConfig {

    private boolean debug;
    private List<RegistryConfig> registries = new ArrayList<>();
    private ConfigSource configSource = ConfigSource.MANUAL;

    @Override
    @JsonDeserialize(contentUsing = JsonRegistryConfigDeserializer.class)
    @JsonSerialize(contentUsing = JsonRegistryConfigSerializer.class)
    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @Override
    public MutableRegistriesConfig mutable() {
        return this;
    }

    @Override
    @JsonIgnore
    public ConfigSource getSource() {
        return configSource;
    }

    @JsonIgnore
    JsonRegistriesConfig setConfigSource(ConfigSource configSource) {
        // package private.
        // Source isn't known during deserialization. Must be added post-construction.
        this.configSource = configSource;
        return this;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        if (registries == null) {
            this.registries.clear();
        } else {
            this.registries = registries;
        }
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
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (debug) {
            buf.append("debug ");
        }
        if (!registries.isEmpty()) {
            RegistryConfig r = registries.get(0);
            buf.append(r);
            for (int i = 1; i < registries.size(); ++i) {
                buf.append(", ").append(registries.get(i));
            }
        }
        return buf.append(']').toString();
    }

    @Override
    public boolean addRegistry(String registryId) {
        final JsonRegistryConfig registry = new JsonRegistryConfig();
        registry.setId(registryId);
        return addRegistry(registry);
    }

    @Override
    public boolean addRegistry(RegistryConfig config) {
        if (registries.stream().anyMatch(r -> r.getId().equals(config.getId()))) {
            return false;
        }
        return registries.add(config);
    }

    @Override
    public boolean removeRegistry(String registryId) {
        return registries.removeIf(r -> r.getId().equals(registryId));
    }

    @Override
    public void persist() {
        throw new UnsupportedOperationException("Not here yet");
    }

    @Override
    public void persist(Path targetFile) throws IOException {
        RegistriesConfigMapperHelper.serialize(this, targetFile);
    }
}
