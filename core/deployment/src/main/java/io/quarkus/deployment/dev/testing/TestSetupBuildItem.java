package io.quarkus.deployment.dev.testing;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Virtual build item that is used to signify that a step must be run to setup
 * continuous testing
 */
public final class TestSetupBuildItem extends MultiBuildItem {

}
