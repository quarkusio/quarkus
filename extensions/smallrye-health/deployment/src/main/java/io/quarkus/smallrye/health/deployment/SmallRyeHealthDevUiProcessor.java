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
            SmallRyeHealthConfig config,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            SmallRyeHealthRecorder unused) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem("Smallrye Health");

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(config.rootPath,
                managementInterfaceBuildTimeConfig, launchModeBuildItem);
        pageBuildItem.addPage(Page.externalPageBuilder("Health")
                .icon("font-awesome-solid:heart-circle-bolt")
                .url(path)
                .isJsonContent());

        pageBuildItem.addPage(Page.externalPageBuilder("Health UI")
                .icon("font-awesome-solid:stethoscope")
                .url(nonApplicationRootPathBuildItem.resolvePath(config.ui.rootPath))
                .isHtmlContent());

        return pageBuildItem;
    }

}
