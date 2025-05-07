package io.quarkus.devui.deployment.menu;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.build.BuildMetricsDevUIRecorder;
import io.quarkus.devui.runtime.build.BuildMetricsJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Build Metrics Page
 */
public class BuildMetricsProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @io.quarkus.deployment.annotations.Record(RUNTIME_INIT)
    public void create(BuildMetricsDevUIRecorder recorder,
            BuildSystemTargetBuildItem buildSystemTarget) {
        recorder.setBuildMetricsPath(buildSystemTarget.getOutputDirectory().resolve("build-metrics.json").toString());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem
                .builder()
                .addBeanClass(BuildMetricsJsonRPCService.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createBuildMetricsPages() {

        InternalPageBuildItem buildMetricsPages = new InternalPageBuildItem("Build Metrics", 50);

        buildMetricsPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-build-metrics")
                .title("Build Steps")
                .icon("font-awesome-solid:hammer")
                .componentLink("qwc-build-steps.js"));

        buildMetricsPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-build-metrics")
                .title("Build Items")
                .icon("font-awesome-solid:trowel")
                .componentLink("qwc-build-items.js"));

        return buildMetricsPages;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem("devui-build-metrics", BuildMetricsJsonRPCService.class);
    }
}
