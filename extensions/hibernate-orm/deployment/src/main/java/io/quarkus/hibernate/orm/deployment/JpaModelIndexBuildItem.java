package io.quarkus.hibernate.orm.deployment;

import org.jboss.jandex.CompositeIndex;

import io.quarkus.builder.item.SimpleBuildItem;

public final class JpaModelIndexBuildItem extends SimpleBuildItem {

    private final CompositeIndex index;

    public JpaModelIndexBuildItem(CompositeIndex index) {
        this.index = index;
    }

    public CompositeIndex getIndex() {
        return index;
    }
}
