package io.quarkus.deployment.builditem;

import org.jboss.jandex.DotName;
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
 * The computing index can also be used to index classes on demand. This when {@link IndexView#getClassByName(DotName)}
 * is called. Note that this is a mutable index as this will add additional information, so in general this Index
 * should only be used if you actually need it.
 *
 * @see AdditionalApplicationArchiveMarkerBuildItem
 * @see IndexDependencyBuildItem
 */
public final class CombinedIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;

    private final IndexView computingIndex;

    public CombinedIndexBuildItem(IndexView index, IndexView computingIndex) {
        this.index = index;
        this.computingIndex = computingIndex;
    }

    public IndexView getIndex() {
        return index;
    }

    public IndexView getComputingIndex() {
        return computingIndex;
    }
}
