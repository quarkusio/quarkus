package io.quarkus.deployment.pkg.steps;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.PackageTypeBuildItem;

/**
 * verifies that the requested package type can actually be produced
 */
public class PackageTypeVerificationBuildStep {

    @BuildStep
    List<PackageTypeBuildItem> builtins() {
        return Arrays.asList(new PackageTypeBuildItem(PackageConfig.NATIVE), new PackageTypeBuildItem(PackageConfig.THIN_JAR),
                new PackageTypeBuildItem(PackageConfig.UBER_JAR));
    }

    @BuildStep
    void verify(List<PackageTypeBuildItem> items, PackageConfig config) {
        Set<String> registered = items.stream().map(PackageTypeBuildItem::getType).collect(Collectors.toSet());
        for (String i : config.types) {
            if (!registered.contains(i)) {
                throw new IllegalStateException("Unknown packaging type " + i + " known types are " + registered);
            }
        }
    }
}
