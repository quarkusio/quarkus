package io.quarkus.devui.deployment.menu;

import java.util.List;
import java.util.Map;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.ExtensionsBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.deployment.extension.ExtensionGroup;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Extensions Page
 */
public class ExtensionsProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createExtensionsPages(ExtensionsBuildItem extensionsBuildItem) {

        InternalPageBuildItem extensionsPages = new InternalPageBuildItem("Extensions", 10);

        // Extensions
        Map<ExtensionGroup, List<Extension>> response = Map.of(
                ExtensionGroup.active, extensionsBuildItem.getActiveExtensions(),
                ExtensionGroup.inactive, extensionsBuildItem.getInactiveExtensions());

        extensionsPages.addBuildTimeData("extensions", response);

        // Page
        extensionsPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-extensions")
                .title("Extensions")
                .icon("font-awesome-solid:puzzle-piece")
                .componentLink("qwc-extensions.js"));

        return extensionsPages;
    }
}