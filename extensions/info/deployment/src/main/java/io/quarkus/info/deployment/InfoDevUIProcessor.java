package io.quarkus.info.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * This processor is responsible for the dev ui widget.
 */
public class InfoDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    MenuPageBuildItem create(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            InfoBuildTimeConfig config,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem) {
        MenuPageBuildItem pageBuildItem = new MenuPageBuildItem();

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(config.path(),
                managementInterfaceBuildTimeConfig, launchModeBuildItem);

        pageBuildItem.addBuildTimeData("infoUrl", path);

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Information")
                .icon("font-awesome-solid:circle-info")
                .componentLink("qwc-info.js"));
        pageBuildItem.addPage(Page.externalPageBuilder("Raw")
                .url(path)
                .icon("font-awesome-solid:circle-info")
                .isJsonContent());

        return pageBuildItem;
    }

}
