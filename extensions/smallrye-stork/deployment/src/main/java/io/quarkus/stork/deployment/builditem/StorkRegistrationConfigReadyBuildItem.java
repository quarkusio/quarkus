package io.quarkus.stork.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item used as a marker to indicate that the automatic service registration
 * configuration has been processed.
 * <p>
 * This build item is produced after checking the application dependencies and
 * preparing the registration configuration. If no explicit configuration is found,
 * a default minimal configuration is created.
 * <p>
 * It triggers the next build step, which initializes Stork.
 */
public final class StorkRegistrationConfigReadyBuildItem extends SimpleBuildItem {

}
