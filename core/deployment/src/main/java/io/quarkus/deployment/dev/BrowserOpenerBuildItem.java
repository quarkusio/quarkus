package io.quarkus.deployment.dev;

import java.util.function.Consumer;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BrowserOpenerBuildItem extends SimpleBuildItem {

    private final Consumer<String> browserOpener;

    public BrowserOpenerBuildItem(Consumer<String> browserOpener) {
        this.browserOpener = browserOpener;
    }

    public Consumer<String> getBrowserOpener() {
        return browserOpener;
    }
}
