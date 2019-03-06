package io.quarkus.camel.core.deployment;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.camel.core.runtime.CamelRuntime;

public final class CamelRuntimeBuildItem extends SimpleBuildItem {
    private final CamelRuntime runtime;

    public CamelRuntimeBuildItem(CamelRuntime runtime) {
        this.runtime = runtime;
    }

    public CamelRuntime getRuntime() {
        return runtime;
    }
}
