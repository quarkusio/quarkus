package io.quarkus.builder;

import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.qlue.item.EmptyClassItem;

/**
 * An item to help bridge the gap between the legacy build item hierarchy and the Qlue item hierarchy.
 */
public final class LegacyEmptyItem extends EmptyClassItem<EmptyBuildItem> {
}
