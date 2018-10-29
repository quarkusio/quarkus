package org.jboss.shamrock.undertow;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.runtime.RuntimeValue;

import io.undertow.servlet.api.DeploymentInfo;

public final class DeploymentInfoBuildItem extends SimpleBuildItem {

    private final RuntimeValue<DeploymentInfo> value;

    public DeploymentInfoBuildItem(RuntimeValue<DeploymentInfo> value) {
        this.value = value;
    }

    public RuntimeValue<DeploymentInfo> getValue() {
        return value;
    }
}
