package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * When this build item has been generated, then the generated main class contains a
 * guard that prevents any of the application's runtime-init {@code StartupTask}s from
 * being run.
 * The guard is activated by setting {@code quarkus.application.pre-boot} system-property to {@code true}
 * and when set, effectively makes the application run the static-init {@code StartupTask}s
 * and exit.
 */
public final class AddPreventRuntimeInitBlockBuildItem extends MultiBuildItem {
}
