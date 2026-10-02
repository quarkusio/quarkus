package io.quarkus.core.deployment.builditem;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * A build result item that causes a build step to be included in all build modes (production, dev, and test).
 * Build steps that should always run regardless of build mode should use
 * {@code @Produce(AlwaysResultBuildItem.class)} to ensure they are included
 * in the build chain.
 * <p>
 * This item is not itself declared as a final build item. Instead, bridge steps consume it and produce the
 * mode-specific result items ({@link ProductionResultBuildItem}, {@link DevResultBuildItem},
 * {@link TestResultBuildItem}), which are declared as final in their respective modes. This causes the
 * bridge step (and transitively, all {@code AlwaysResultBuildItem} producers) to be included whenever any
 * mode-specific result item is final.
 *
 * @see ProductionResultBuildItem
 * @see DevResultBuildItem
 * @see TestResultBuildItem
 */
public final class AlwaysResultBuildItem extends EmptyBuildItem {
    private AlwaysResultBuildItem() {
    }
}
