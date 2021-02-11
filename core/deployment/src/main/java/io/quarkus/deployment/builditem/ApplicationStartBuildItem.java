package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A symbolic class that is produced after the startup event has been fired.
 * 
 * At this point it should be safe to open sockets and begin processing requests
 */
public final class ApplicationStartBuildItem extends SimpleBuildItem {

}
