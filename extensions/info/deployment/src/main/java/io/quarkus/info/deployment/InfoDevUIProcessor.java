package io.quarkus.info.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * This processor is responsible for the dev ui widget.
 */
public class InfoDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void create(BuildProducer<MenuPageBuildItem> menuPageProducer,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            InfoBuildTimeConfig config,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem) {

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(config.path(),
                managementInterfaceBuildTimeConfig, launchModeBuildItem);

        MenuPageBuildItem menuBuildItem = new MenuPageBuildItem();

        menuBuildItem.addBuildTimeData("infoUrl", path);

        WebComponentPageBuilder infoPage = Page.webComponentPageBuilder()
                .title("Information")
                .icon("font-awesome-solid:circle-info")
                .componentLink("qwc-info.js");

        menuBuildItem.addPage(infoPage);
        menuBuildItem.addPage(Page.externalPageBuilder("Raw")
                .url(path)
                .icon("font-awesome-solid:circle-info")
                .isJsonContent());

        menuPageProducer.produce(menuBuildItem);

        CardPageBuildItem cardBuildItem = new CardPageBuildItem();
        cardBuildItem.addPage(infoPage);
        cardPageProducer.produce(cardBuildItem);
    }

}
