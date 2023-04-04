package io.quarkus.deployment.pkg.steps;

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
        return List.of(
                new PackageTypeBuildItem(PackageConfig.BuiltInType.NATIVE.getValue()),
                new PackageTypeBuildItem(PackageConfig.BuiltInType.NATIVE_SOURCES.getValue()),
                new PackageTypeBuildItem(PackageConfig.BuiltInType.JAR.getValue()),
                new PackageTypeBuildItem(PackageConfig.BuiltInType.FAST_JAR.getValue()),
                new PackageTypeBuildItem(PackageConfig.BuiltInType.LEGACY_JAR.getValue()),
                new PackageTypeBuildItem(PackageConfig.BuiltInType.UBER_JAR.getValue()),
                new PackageTypeBuildItem(PackageConfig.BuiltInType.MUTABLE_JAR.getValue()));
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
