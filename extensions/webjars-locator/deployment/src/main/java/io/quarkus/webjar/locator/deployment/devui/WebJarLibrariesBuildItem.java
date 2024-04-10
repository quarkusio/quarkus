package io.quarkus.webjar.locator.deployment.devui;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class WebJarLibrariesBuildItem extends MultiBuildItem {
    private final String provider;
    private final List<WebJarLibrary> webJarLibraries;

    public WebJarLibrariesBuildItem(String provider, List<WebJarLibrary> webJarLibraries) {
        this.provider = provider;
        this.webJarLibraries = webJarLibraries;
    }

    public List<WebJarLibrary> getWebJarLibraries() {
        return this.webJarLibraries;
    }

    public String getProvider() {
        return this.provider;
    }
}
