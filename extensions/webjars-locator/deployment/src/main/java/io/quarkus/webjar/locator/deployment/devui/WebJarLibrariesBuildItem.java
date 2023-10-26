package io.quarkus.webjar.locator.deployment.devui;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class WebJarLibrariesBuildItem extends SimpleBuildItem {

    private final List<WebJarLibrary> webJarLibraries;

    public WebJarLibrariesBuildItem(List<WebJarLibrary> webJarLibraries) {
        this.webJarLibraries = webJarLibraries;
    }

    public List<WebJarLibrary> getWebJarLibraries() {
        return webJarLibraries;
    }
}
