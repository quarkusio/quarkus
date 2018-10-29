package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

public final class RuntimeInitializedClassBuildItem extends MultiBuildItem {

    private final String className;

    public RuntimeInitializedClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
