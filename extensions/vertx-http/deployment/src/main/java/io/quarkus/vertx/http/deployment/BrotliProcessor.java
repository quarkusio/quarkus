package io.quarkus.vertx.http.deployment;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.graal.Brotli4jFeature;

class BrotliProcessor {

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void validateBrotli(VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        if (isBrotliEnabled(httpBuildTimeConfig) && !isBrotli4jPresent()) {
            throw new ConfigurationException(
                    "quarkus.http.compressors contains 'br' but Brotli4j is not available. Add the"
                            + " com.aayushatharva.brotli4j:brotli4j dependency to your project. With Gradle, or when"
                            + " building a native executable in a container, also add the"
                            + " com.aayushatharva.brotli4j:native-<os>-<arch> artifact matching the target platform.");
        }
    }

    @BuildStep
    NativeImageFeatureBuildItem brotli4jFeature() {
        // Not tied to quarkus.http.compressors: Netty also uses Brotli4j to decompress when it is present
        if (isBrotli4jPresent()) {
            return new NativeImageFeatureBuildItem(Brotli4jFeature.class.getName());
        }
        return null;
    }

    @BuildStep
    ModuleEnableNativeAccessBuildItem brotli4jEnableNativeAccess() {
        if (isBrotli4jPresent()) {
            return new ModuleEnableNativeAccessBuildItem("com.aayushatharva.brotli4j");
        }
        return null;
    }

    private static boolean isBrotli4jPresent() {
        return QuarkusClassLoader.isClassPresentAtRuntime("com.aayushatharva.brotli4j.Brotli4jLoader");
    }

    private static boolean isBrotliEnabled(VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        return httpBuildTimeConfig.compressors().isPresent()
                && httpBuildTimeConfig.compressors().get().stream().anyMatch(s -> s.equalsIgnoreCase("br"));
    }
}
