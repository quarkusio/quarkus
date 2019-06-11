package io.quarkus.builder;

import java.util.Objects;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.NamedBuildItem;
import io.quarkus.builder.item.NamedMultiBuildItem;

/**
 */
final class ItemId {
    private final Class<? extends BuildItem> itemType;
    private final Object name;

    ItemId(final Class<? extends BuildItem> itemType, final Object name) {
        Assert.checkNotNullParam("itemType", itemType);
        if (NamedBuildItem.class.isAssignableFrom(itemType)) {
            // todo: support default names
            Assert.checkNotNullParam("name", name);
        }
        this.itemType = itemType;
        this.name = name;
    }

    boolean isMulti() {
        return MultiBuildItem.class.isAssignableFrom(itemType) || NamedMultiBuildItem.class.isAssignableFrom(itemType);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemId && equals((ItemId) obj);
    }

    boolean equals(ItemId obj) {
        return this == obj || obj != null && itemType == obj.itemType && Objects.equals(name, obj.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name) * 31 + Objects.hashCode(itemType);
    }

    @Override
    public String toString() {
        final Object name = this.name;
        final Class<? extends BuildItem> itemType = this.itemType;
        if (name == null) {
            assert itemType != null;
            return itemType.toString();
        } else if (itemType == null) {
            assert name != null;
            return "name " + name.toString();
        } else {
            return itemType.toString() + " with name " + name;
        }
    }

    Class<? extends BuildItem> getType() {
        return itemType;
    }
}
