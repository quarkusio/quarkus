package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

public final class MainClassBuildItem extends SimpleBuildItem {

    public final String className;

    public MainClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
