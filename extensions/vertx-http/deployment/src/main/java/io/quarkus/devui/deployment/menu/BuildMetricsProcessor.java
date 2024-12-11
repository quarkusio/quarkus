package io.quarkus.devui.deployment.menu;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Build Metrics Page
 */
public class BuildMetricsProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
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
}
