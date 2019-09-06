package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker interface indicating the additional routes have been installed.
 * <p>
 * It allows to install the default route at the end.
 */
public final class AdditionalRoutesInstalledBuildItem extends MultiBuildItem {

}
