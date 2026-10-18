package io.quarkus.vertx.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker build item used to force the re-initialization of Vert.x JSON handling in native-image
 */
public final class ReinitializeVertxJsonBuildItem extends MultiBuildItem {
}
