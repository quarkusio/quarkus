package io.quarkus.devui.deployment.menu;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates DevServices Page
 */
public class DevServicesProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createDevServicesPages(List<DevServiceDescriptionBuildItem> devServiceDescriptions) {

        InternalPageBuildItem devServicesPages = new InternalPageBuildItem("Dev Services", 40);

        devServicesPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-dev-services")
                .title("Dev services")
                .icon("font-awesome-solid:wand-magic-sparkles")
                .componentLink("qwc-dev-services.js"));

        devServicesPages.addBuildTimeData("devServices", devServiceDescriptions);

        return devServicesPages;

    }
}