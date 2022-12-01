package io.quarkus.builder;

import java.util.Objects;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;

/**
 */
final class ItemId {
    private final Class<? extends BuildItem> itemType;

    ItemId(final Class<? extends BuildItem> itemType) {
        Assert.checkNotNullParam("itemType", itemType);
        this.itemType = itemType;
    }

    boolean isMulti() {
        return MultiBuildItem.class.isAssignableFrom(itemType);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemId && equals((ItemId) obj);
    }

    boolean equals(ItemId obj) {
        return this == obj || obj != null && itemType == obj.itemType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(itemType);
    }

    @Override
    public String toString() {
        return itemType.toString();
    }

    Class<? extends BuildItem> getType() {
        return itemType;
    }
}
