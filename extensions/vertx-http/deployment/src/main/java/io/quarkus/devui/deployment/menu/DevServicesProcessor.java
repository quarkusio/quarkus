package io.quarkus.devui.deployment.menu;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates DevServices Page
 */
public class DevServicesProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createDevServicesPages(List<DevServiceDescriptionBuildItem> devServiceDescriptions,
            List<DevServicesResultBuildItem> devServicesResultBuildItems) {

        List<DevServiceDescriptionBuildItem> otherDevServices = getOtherDevServices(devServicesResultBuildItems);

        InternalPageBuildItem devServicesPages = new InternalPageBuildItem("Dev Services", 40);

        devServicesPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-dev-services")
                .title("Dev services")
                .icon("font-awesome-solid:wand-magic-sparkles")
                .componentLink("qwc-dev-services.js"));

        List<DevServiceDescriptionBuildItem> combined = new ArrayList<>();

        if (!devServiceDescriptions.isEmpty()) {
            combined.addAll(devServiceDescriptions);
        }
        if (!otherDevServices.isEmpty()) {
            combined.addAll(otherDevServices);
        }

        devServicesPages.addBuildTimeData("devServices", combined);

        return devServicesPages;

    }

    private List<DevServiceDescriptionBuildItem> getOtherDevServices(
            List<DevServicesResultBuildItem> devServicesResultBuildItems) {
        List<DevServiceDescriptionBuildItem> devServiceDescriptions = new ArrayList<>();
        for (DevServicesResultBuildItem devServicesResultBuildItem : devServicesResultBuildItems) {
            if (devServicesResultBuildItem.getContainerId() == null) {
                devServiceDescriptions.add(new DevServiceDescriptionBuildItem(devServicesResultBuildItem.getName(), null,
                        devServicesResultBuildItem.getConfig()));
            }
        }
        return devServiceDescriptions;
    }
}
