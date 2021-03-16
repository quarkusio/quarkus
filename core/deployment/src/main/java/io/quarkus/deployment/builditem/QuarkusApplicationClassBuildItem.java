package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.QuarkusApplication;

public final class QuarkusApplicationClassBuildItem extends SimpleBuildItem {
    private final String className;

    public QuarkusApplicationClassBuildItem(Class<? extends QuarkusApplication> quarkusApplicationClass) {
        this.className = quarkusApplicationClass.getName();
    }

    public String getClassName() {
        return className;
    }
}
