package io.quarkus.webjar.locator.deployment.devui;

import java.util.List;

public class WebJarAsset {

    private String name;
    private List<WebJarAsset> children;
    private boolean fileAsset;
    private String urlPart;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WebJarAsset> getChildren() {
        return children;
    }

    public void setChildren(List<WebJarAsset> children) {
        this.children = children;
    }

    public boolean isFileAsset() {
        return fileAsset;
    }

    public void setFileAsset(boolean fileAsset) {
        this.fileAsset = fileAsset;
    }

    public String getUrlPart() {
        return urlPart;
    }

    public void setUrlPart(String urlPart) {
        this.urlPart = urlPart;
    }
}
