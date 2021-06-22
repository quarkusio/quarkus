package io.quarkus.mongodb.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * If generated, all the Mongo clients need to be unremovable
 *
 * @deprecated use MongoUnremovableSyncClientsBuildItem and/or MongoUnremovableReactiveClientsBuildItem instead.
 */
@Deprecated
public final class MongoUnremovableClientsBuildItem extends MultiBuildItem {

}
