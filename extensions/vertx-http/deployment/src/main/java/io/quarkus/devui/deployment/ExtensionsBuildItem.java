package io.quarkus.devui.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devui.deployment.extension.Extension;

public final class ExtensionsBuildItem extends SimpleBuildItem {

    private final List<Extension> activeExtensions;
    private final List<Extension> inactiveExtensions;
    private final List<Extension> sectionMenuExtensions;
    private final List<Extension> footerTabsExtensions;

    public ExtensionsBuildItem(List<Extension> activeExtensions, List<Extension> inactiveExtensions,
            List<Extension> sectionMenuExtensions, List<Extension> footerTabsExtensions) {
        this.activeExtensions = activeExtensions;
        this.inactiveExtensions = inactiveExtensions;
        this.sectionMenuExtensions = sectionMenuExtensions;
        this.footerTabsExtensions = footerTabsExtensions;
    }

    public List<Extension> getActiveExtensions() {
        return this.activeExtensions;
    }

    public List<Extension> getInactiveExtensions() {
        return this.inactiveExtensions;
    }

    public List<Extension> getSectionMenuExtensions() {
        return this.sectionMenuExtensions;
    }

    public List<Extension> getFooterTabsExtensions() {
        return this.footerTabsExtensions;
    }
}
