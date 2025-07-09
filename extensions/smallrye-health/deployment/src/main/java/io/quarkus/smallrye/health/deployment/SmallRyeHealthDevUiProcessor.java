package io.quarkus.smallrye.health.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.health.runtime.SmallRyeHealthRecorder;
import io.quarkus.smallrye.health.runtime.dev.ui.HealthJsonRPCService;
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
        pageBuildItem.setLogo("smallrye_dark.svg", "smallrye_light.svg");
        pageBuildItem.addLibraryVersion("io.smallrye", "smallrye-health", "SmallRye Health",
                "https://github.com/smallrye/smallrye-health/");
        pageBuildItem.addLibraryVersion("org.eclipse.microprofile.health", "microprofile-health-api", "MicroProfile Health",
                "https://github.com/microprofile/microprofile-health");

        String path = nonApplicationRootPathBuildItem.resolveManagementPath(config.rootPath(),
                managementBuildTimeConfig, launchModeBuildItem, config.managementEnabled());

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Health")
                .icon("font-awesome-solid:stethoscope")
                .componentLink("qwc-smallrye-health-ui.js")
                .dynamicLabelJsonRPCMethodName("getStatus")
                .streamingLabelJsonRPCMethodName("streamStatus", "interval"));

        pageBuildItem.addPage(Page.externalPageBuilder("Raw")
                .icon("font-awesome-solid:heart-circle-bolt")
                .url(path, path)
                .isJsonContent());

        return pageBuildItem;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HealthJsonRPCService.class);
    }
}
