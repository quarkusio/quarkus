package io.quarkus.jlink.deployment;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildProvider;
import io.quarkus.jlink.spi.JLinkImageBuildItem;

/**
 * A build provider which causes the jlink image to be built.
 * This is a provided service.
 */
public final class JLinkBuildProvider implements BuildProvider {
    public void installInto(final BuildChainBuilder builder) {
        builder.addFinal(JLinkImageBuildItem.class);
    }
}
