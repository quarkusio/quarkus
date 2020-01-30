package io.quarkus.deployment.builditem;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * An index of application classes which is built from archives and dependencies that contain a certain marker file.
 * These files include but are not limited to - beans.xml, jandex.idx and config properties.
 * Additional marker files can be declared via {@link AdditionalApplicationArchiveMarkerBuildItem}.
 * Alternatively, you can index a dependency through {@link IndexDependencyBuildItem}.
 *
 * Compared to {@code BeanArchiveIndexBuildItem}, this index doesn't contain all CDI-related information.
 * On the other hand, it can contain classes from archives/dependencies that had no CDI component declared within them.
 *
 * @see AdditionalApplicationArchiveMarkerBuildItem
 * @see IndexDependencyBuildItem
 */
public final class CombinedIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;

    public CombinedIndexBuildItem(IndexView index) {
        this.index = index;
    }

    public IndexView getIndex() {
        return index;
    }
}
