package org.jboss.shamrock.undertow;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.runtime.RuntimeValue;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

public final class ServletExtensionBuildItem extends MultiBuildItem {

    private final ServletExtension value;

    public ServletExtensionBuildItem(ServletExtension value) {
        this.value = value;
    }

    public ServletExtension getValue() {
        return value;
    }
}
