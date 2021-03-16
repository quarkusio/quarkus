package io.quarkus.deployment.pkg.steps;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.PackageTypeBuildItem;

/**
 * Verifies that the requested package type can actually be produced
 */
public class PackageTypeVerificationBuildStep {

    @BuildStep
    List<PackageTypeBuildItem> builtins() {
        return Arrays.asList(new PackageTypeBuildItem(PackageConfig.NATIVE),
                new PackageTypeBuildItem(PackageConfig.NATIVE_SOURCES),
                new PackageTypeBuildItem(PackageConfig.JAR), new PackageTypeBuildItem(PackageConfig.FAST_JAR),
                new PackageTypeBuildItem(PackageConfig.LEGACY_JAR),
                new PackageTypeBuildItem(PackageConfig.UBER_JAR),
                new PackageTypeBuildItem(PackageConfig.MUTABLE_JAR));
    }

    @BuildStep
    ServiceStartBuildItem verify(List<PackageTypeBuildItem> items, PackageConfig config) {
        Set<String> registered = items.stream().map(PackageTypeBuildItem::getType).collect(Collectors.toSet());
        if (!registered.contains(config.type)) {
            throw new IllegalStateException("Unknown packaging type '" + config.type + "' known types are " + registered);
        }
        return null;
    }
}
