package io.quarkus.devui.spi.welcome;

import io.quarkus.devjsonrpc.spi.AbstractDevBuildItem;

/**
 * Adds dynamic data to the welcome page
 */
public final class DynamicWelcomeBuildItem extends AbstractDevBuildItem {

    private final String html;

    public DynamicWelcomeBuildItem(String html) {
        super();
        this.html = html;
    }

    public String getHTML() {
        return this.html;
    }
}
