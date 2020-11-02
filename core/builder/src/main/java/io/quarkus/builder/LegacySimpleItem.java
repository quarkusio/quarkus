package io.quarkus.builder;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qlue.item.SimpleClassItem;
import io.smallrye.common.constraint.Assert;

/**
 * An item to help bridge the gap between the legacy build item hierarchy and the Qlue item hierarchy.
 */
public final class LegacySimpleItem extends SimpleClassItem<SimpleBuildItem> {
    private final SimpleBuildItem item;

    public LegacySimpleItem(final SimpleBuildItem item) {
        this.item = Assert.checkNotNullParam("item", item);
    }

    public SimpleBuildItem getItem() {
        return item;
    }
}
