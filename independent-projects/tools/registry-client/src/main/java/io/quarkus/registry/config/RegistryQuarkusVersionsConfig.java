package io.quarkus.registry.config;

public interface RegistryQuarkusVersionsConfig {

    String getRecognizedVersionsExpression();

    boolean isExclusiveProvider();
}
