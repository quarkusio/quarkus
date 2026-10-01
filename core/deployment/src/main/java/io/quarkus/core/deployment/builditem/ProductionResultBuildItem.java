package io.quarkus.core.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * A build result item that is declared as a final build item during production builds.
 * Build steps that should always run during a production build should use
 * {@code @Produce(ProductionResultBuildItem.class)} to ensure they are included
 * in the build chain.
 *
 * @see AlwaysResultBuildItem
 * @see DevResultBuildItem
 * @see TestResultBuildItem
 */
public final class ProductionResultBuildItem extends EmptyBuildItem {
    private ProductionResultBuildItem() {
    }
}
