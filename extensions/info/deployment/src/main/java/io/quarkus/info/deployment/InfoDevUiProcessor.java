package io.quarkus.info.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.info.runtime.InfoRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * This processor is responsible for the dev ui widget.
 */
public class InfoDevUiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    CardPageBuildItem create(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            InfoBuildTimeConfig config,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem,
            InfoRecorder unused) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(config.path(),
                managementInterfaceBuildTimeConfig, launchModeBuildItem);
        pageBuildItem.addPage(Page.externalPageBuilder("App Information")
                .icon("font-awesome-solid:circle-info")
                .url(path)
                .isJsonContent());

        return pageBuildItem;
    }

}
