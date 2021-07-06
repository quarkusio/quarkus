package io.quarkus.registry.config.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonRegistriesConfig implements RegistriesConfig {

    private boolean debug;
    private List<RegistryConfig> registries = Collections.emptyList();

    @Override
    @JsonDeserialize(contentUsing = JsonRegistryConfigDeserializer.class)
    @JsonSerialize(contentUsing = JsonRegistryConfigSerializer.class)
    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        this.registries = registries == null ? Collections.emptyList() : registries;
    }

    public void addRegistry(RegistryConfig registry) {
        if (registries.isEmpty()) {
            registries = new ArrayList<>();
        }
        registries.add(registry);
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
}
