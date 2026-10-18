package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker build item that can be used by advanced extensions that can make
 * sure that mixing RESTEasy Classic and RESTEasy Reactive dependencies
 * will not cause problems
 */
public final class IgnoreStackMixingBuildItem extends MultiBuildItem {
}
