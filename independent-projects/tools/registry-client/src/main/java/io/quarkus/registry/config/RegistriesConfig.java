package io.quarkus.registry.config;

import java.util.List;

public interface RegistriesConfig {

    boolean isDebug();

    List<RegistryConfig> getRegistries();
}
