package io.quarkus.azure.functions.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

final public class AzureFunctionsAppNameBuildItem extends SimpleBuildItem {
    private final String appName;

    public AzureFunctionsAppNameBuildItem(String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }
}
