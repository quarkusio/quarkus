package io.quarkus.smallrye.metrics.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.metrics.runtime.SmallRyeMetricsRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * This processor is responsible for the dev ui widget.
 */
public class SmallRyeMetricsDevUiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    CardPageBuildItem create(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            SmallRyeMetricsConfig config,
            LaunchModeBuildItem launchModeBuildItem,
            SmallRyeMetricsRecorder unused) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        var path = nonApplicationRootPathBuildItem.resolveManagementPath(config.path(),
                managementBuildTimeConfig, launchModeBuildItem);
        pageBuildItem.addPage(Page.externalPageBuilder("All Metrics")
                .icon("font-awesome-solid:chart-line")
                .url(path));

        pageBuildItem.addPage(Page.externalPageBuilder("Vendor Metrics")
                .icon("font-awesome-solid:chart-line")
                .url(path + "/vendor"));

        pageBuildItem.addPage(Page.externalPageBuilder("Application Metrics")
                .icon("font-awesome-solid:chart-line")
                .url(path + "/application"));

        pageBuildItem.addPage(Page.externalPageBuilder("Base Metrics")
                .icon("font-awesome-solid:chart-line")
                .url(path + "/base"));

        return pageBuildItem;
    }

}
