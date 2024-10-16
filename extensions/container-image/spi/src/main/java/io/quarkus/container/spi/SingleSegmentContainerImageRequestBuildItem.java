package io.quarkus.container.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * There are cases where a single segment image (an image without group) is preferred.
 * This build item is used to express this preferrence.
 **/
public final class SingleSegmentContainerImageRequestBuildItem extends SimpleBuildItem {
}
