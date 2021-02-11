package io.quarkus.deployment.builditem;

import org.jboss.jandex.Index;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The Jandex index of the application root
 */
public final class ApplicationIndexBuildItem extends SimpleBuildItem {

    private final Index index;

    public ApplicationIndexBuildItem(Index index) {
        this.index = index;
    }

    public Index getIndex() {
        return index;
    }
}
