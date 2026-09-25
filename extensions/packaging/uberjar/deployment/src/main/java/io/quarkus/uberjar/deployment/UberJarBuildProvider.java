package io.quarkus.uberjar.deployment;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildProvider;
import io.quarkus.uberjar.spi.UberJarBuildItem;

/**
 * A build provider which causes the modular UberJar to be built.
 * This is a provided service.
 */
public final class UberJarBuildProvider implements BuildProvider {
    @Override
    public void installInto(final BuildChainBuilder builder) {
        builder.addFinal(UberJarBuildItem.class);
    }
}
