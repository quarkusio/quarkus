package io.quarkus.resteasy.reactive.server.spi;

import java.util.Objects;

import org.jboss.resteasy.reactive.common.processor.TargetJavaVersion;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This build item is only used for testing purposes to override the default behavior
 */
public final class TargetJavaVersionBuildItem extends SimpleBuildItem {

    private final TargetJavaVersion targetJavaVersion;

    public TargetJavaVersionBuildItem(TargetJavaVersion targetJavaVersion) {
        this.targetJavaVersion = Objects.requireNonNull(targetJavaVersion);
    }

    public TargetJavaVersion getTargetJavaVersion() {
        return targetJavaVersion;
    }
}
