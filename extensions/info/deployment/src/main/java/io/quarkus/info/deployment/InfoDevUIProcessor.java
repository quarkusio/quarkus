package io.quarkus.info.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.ExternalPageBuilder;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * This processor is responsible for the dev ui widget.
 */
public class InfoDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void create(BuildProducer<CardPageBuildItem> cardPageProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            InfoBuildTimeConfig config,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem) {

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(config.path(),
                managementInterfaceBuildTimeConfig, launchModeBuildItem);

        WebComponentPageBuilder infoPage = Page.webComponentPageBuilder()
                .title("Information")
                .icon("font-awesome-solid:circle-info")
                .componentLink("qwc-info.js");

        ExternalPageBuilder rawPage = Page.externalPageBuilder("Raw")
                .url(path)
                .icon("font-awesome-solid:circle-info")
                .isJsonContent();

        CardPageBuildItem cardBuildItem = new CardPageBuildItem();
        cardBuildItem.addBuildTimeData("infoUrl", path);
        cardBuildItem.addPage(infoPage);
        cardBuildItem.addPage(rawPage);
        cardPageProducer.produce(cardBuildItem);
    }

}
