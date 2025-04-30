package io.quarkus.smallrye.health.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * This processor is responsible for the dev ui widget.
 */
public class SmallRyeHealthDevUiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    CardPageBuildItem create(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SmallRyeHealthBuildTimeConfig config,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            SmallRyeHealthRecorder unused) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        String path = nonApplicationRootPathBuildItem.resolveManagementPath(config.rootPath(),
                managementBuildTimeConfig, launchModeBuildItem, config.managementEnabled());

        pageBuildItem.addPage(Page.externalPageBuilder("Health")
                .icon("font-awesome-solid:heart-circle-bolt")
                .url(path, path)
                .isJsonContent());

        String uipath = nonApplicationRootPathBuildItem.resolveManagementPath(config.ui().rootPath(),
                managementBuildTimeConfig, launchModeBuildItem, config.managementEnabled());
        pageBuildItem.addPage(Page.externalPageBuilder("Health UI")
                .icon("font-awesome-solid:stethoscope")
                .url(uipath, uipath)
                .isHtmlContent());

        return pageBuildItem;
    }

}
