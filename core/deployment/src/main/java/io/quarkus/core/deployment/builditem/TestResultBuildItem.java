package io.quarkus.core.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * A build result item that is declared as a final build item during test builds.
 * Build steps that should always run during a test build should use
 * {@code @Produce(TestResultBuildItem.class)} to ensure they are included
 * in the build chain.
 *
 * @see AlwaysResultBuildItem
 * @see DevResultBuildItem
 * @see ProductionResultBuildItem
 */
public final class TestResultBuildItem extends EmptyBuildItem {
    private TestResultBuildItem() {
    }
}
