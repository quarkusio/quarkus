package io.quarkus.hibernate.orm.deployment;

import org.jboss.jandex.CompositeIndex;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Provides the Jandex index of the application, combined with the index
 * of additional JPA components that might have been generated.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class JpaModelIndexBuildItem extends SimpleBuildItem {

    private final CompositeIndex index;

    public JpaModelIndexBuildItem(CompositeIndex index) {
        this.index = index;
    }

    public CompositeIndex getIndex() {
        return index;
    }
}
