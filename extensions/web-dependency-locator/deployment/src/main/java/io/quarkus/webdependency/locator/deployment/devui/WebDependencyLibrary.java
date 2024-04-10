package io.quarkus.webdependency.locator.deployment.devui;

public class WebDependencyLibrary {

    private final String webDependencyName;
    private String version;
    private WebDependencyAsset rootAsset; // must be a list to work with vaadin-grid

    public WebDependencyLibrary(String webDependencyName) {
        this.webDependencyName = webDependencyName;
    }

    public String getWebDependencyName() {
        return webDependencyName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public WebDependencyAsset getRootAsset() {
        return rootAsset;
    }

    public void setRootAsset(WebDependencyAsset rootAsset) {
        this.rootAsset = rootAsset;
    }
}
