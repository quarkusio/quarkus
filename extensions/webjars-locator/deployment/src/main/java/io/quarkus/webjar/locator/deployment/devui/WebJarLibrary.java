package io.quarkus.webjar.locator.deployment.devui;

public class WebJarLibrary {

    private final String webJarName;
    private String version;
    private WebJarAsset rootAsset; // must be a list to work with vaadin-grid

    public WebJarLibrary(String webJarName) {
        this.webJarName = webJarName;
    }

    public String getWebJarName() {
        return webJarName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public WebJarAsset getRootAsset() {
        return rootAsset;
    }

    public void setRootAsset(WebJarAsset rootAsset) {
        this.rootAsset = rootAsset;
    }
}
