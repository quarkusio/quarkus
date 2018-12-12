/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.builder;

import java.util.Objects;

import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.NamedBuildItem;
import org.jboss.builder.item.NamedMultiBuildItem;
import org.wildfly.common.Assert;

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
