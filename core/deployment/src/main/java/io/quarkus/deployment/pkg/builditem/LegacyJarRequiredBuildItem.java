package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Ignored.
 * To force legacy-JAR creation, implement a configuration customizer which resets the
 * default {@code quarkus.package.jar.type} to {@code legacy-jar},
 * and add a validation step to your processor which verifies that {@code legacy-jar}
 * is selected, throwing an error otherwise.
 */
@Deprecated(forRemoval = true)
public final class LegacyJarRequiredBuildItem extends MultiBuildItem {
}
