package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Ignored.
 * To force uber-JAR creation, implement a configuration customizer which resets the
 * default {@code quarkus.package.jar.type} to {@code uber-jar},
 * and add a validation step to your processor which verifies that {@code uber-jar}
 * is selected, throwing an error otherwise.
 */
@Deprecated(forRemoval = true)
public final class UberJarRequiredBuildItem extends MultiBuildItem {
}
