package io.quarkus.jlink.deployment;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildProvider;
import io.quarkus.jlink.spi.JLinkImageBuildItem;

public final class JLinkBuildProvider implements BuildProvider {
    public void installInto(final BuildChainBuilder builder) {
        builder.addFinal(JLinkImageBuildItem.class);
    }
}
