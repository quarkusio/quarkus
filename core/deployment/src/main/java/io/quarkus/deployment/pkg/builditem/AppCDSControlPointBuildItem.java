package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * If this build item is generated, then it means that there is a recorder step that can be used as a point at which
 * the application loading during AppCDS generation can be stopped safely, therefore allowing Quarkus to not have to
 * stop loading the application in the static init phase which is the default approach for AppCDS generation.
 */
public final class AppCDSControlPointBuildItem extends SimpleBuildItem {
}
