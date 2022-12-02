package io.quarkus.builder;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 */
public enum ProduceFlag {
    /**
     * Only produce this item weakly: if only weak items produced by a build step are consumed, the step will not be included.
     */
    WEAK,
    /**
     * Only produce this {@link SimpleBuildItem} if no other build steps produce it.
     */
    OVERRIDABLE,
}
