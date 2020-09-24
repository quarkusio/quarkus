package io.quarkus.deployment.builditem;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item represents index with classes that have been generated during
 * build by extension. It should only be used by extension that produce
 * addition classes that should be visible to core components such as
 * <code>ReflectiveHierarchyStep</code>.
 *
 */
public final class GeneratedClassesIndexBuildItem extends MultiBuildItem {

    private final IndexView index;

    public GeneratedClassesIndexBuildItem(IndexView index) {
        this.index = index;
    }

    public IndexView getIndex() {
        return index;
    }
}
