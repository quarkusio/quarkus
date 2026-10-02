package io.quarkus.core.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * A build result item that is declared as a final build item during dev-mode builds.
 * Build steps that should always run during a dev-mode build should use
 * {@code @Produce(DevResultBuildItem.class)} to ensure they are included
 * in the build chain.
 *
 * @see AlwaysResultBuildItem
 * @see ProductionResultBuildItem
 * @see TestResultBuildItem
 */
public final class DevResultBuildItem extends EmptyBuildItem {
    private DevResultBuildItem() {
    }
}
