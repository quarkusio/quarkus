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

@JsonInclude(JsonInclude.Include.NON_NULL)
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
}
