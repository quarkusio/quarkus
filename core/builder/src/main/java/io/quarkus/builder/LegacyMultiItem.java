package io.quarkus.builder;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qlue.item.MultiClassItem;
import io.smallrye.common.constraint.Assert;

/**
 * An item to help bridge the gap between the legacy build item hierarchy and the Qlue item hierarchy.
 */
public final class LegacyMultiItem extends MultiClassItem<MultiBuildItem> {
    private final MultiBuildItem item;

    public LegacyMultiItem(final MultiBuildItem item) {
        this.item = Assert.checkNotNullParam("item", item);
    }

    public MultiBuildItem getItem() {
        return item;
    }
}
