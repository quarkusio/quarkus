package org.jboss.shamrock.camel.deployment;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.camel.runtime.CamelRuntime;

public final class CamelRuntimeBuildItem extends SimpleBuildItem {
    private final CamelRuntime runtime;

    public CamelRuntimeBuildItem(CamelRuntime runtime) {
        this.runtime = runtime;
    }

    public CamelRuntime getRuntime() {
        return runtime;
    }
}
