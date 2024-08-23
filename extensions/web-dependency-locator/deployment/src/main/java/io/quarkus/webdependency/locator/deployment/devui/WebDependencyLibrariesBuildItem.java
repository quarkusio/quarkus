package io.quarkus.webdependency.locator.deployment.devui;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class WebDependencyLibrariesBuildItem extends MultiBuildItem {
    private final String provider;
    private final List<WebDependencyLibrary> webDependencyLibraries;

    public WebDependencyLibrariesBuildItem(String provider, List<WebDependencyLibrary> webDependencyLibraries) {
        this.provider = provider;
        this.webDependencyLibraries = webDependencyLibraries;
    }

    public List<WebDependencyLibrary> getWebDependencyLibraries() {
        return this.webDependencyLibraries;
    }

    public String getProvider() {
        return this.provider;
    }
}
