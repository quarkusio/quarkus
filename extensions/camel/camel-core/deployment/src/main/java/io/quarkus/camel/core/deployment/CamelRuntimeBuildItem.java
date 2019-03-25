package io.quarkus.camel.core.deployment;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.camel.core.runtime.CamelRuntime;
import io.quarkus.runtime.RuntimeValue;

public final class CamelRuntimeBuildItem extends SimpleBuildItem {

    private final RuntimeValue<CamelRuntime> runtime;

    public CamelRuntimeBuildItem(RuntimeValue<CamelRuntime> runtime) {
        this.runtime = runtime;
    }

    public RuntimeValue<CamelRuntime> getRuntime() {
        return runtime;
    }
}
