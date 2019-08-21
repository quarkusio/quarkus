package io.quarkus.caffeine.deployment;

import java.io.IOException;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public class CaffeineProcessor {

    @BuildStep
    ReflectiveClassBuildItem cacheClasses() throws IOException {
        //todo: how to we want to handle this? There are a lot of different cache classes
        return new ReflectiveClassBuildItem(false, false,
                "com.github.benmanes.caffeine.cache.SSLMS",
                "com.github.benmanes.caffeine.cache.SILMS",
                "com.github.benmanes.caffeine.cache.PSMS",
                "com.github.benmanes.caffeine.cache.PDMS",
                "com.github.benmanes.caffeine.cache.SILMS",
                "com.github.benmanes.caffeine.cache.SSMS",
                "com.github.benmanes.caffeine.cache.SSLA",
                "com.github.benmanes.caffeine.cache.PSA");
    }
}
