package io.quarkus.devui.deployment.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates DevServices Page
 */
public class DevServicesProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    InternalPageBuildItem createDevServicesPages(List<DevServiceDescriptionBuildItem> devServiceDescriptions,
            List<DevServicesResultBuildItem> devServicesResultBuildItems) {

        List<DevServiceDescriptionBuildItem> otherDevServices = getOtherDevServices(devServicesResultBuildItems);

        InternalPageBuildItem devServicesPages = new InternalPageBuildItem("Dev Services", 40);

        devServicesPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-dev-services")
                .title("Dev services")
                .icon("font-awesome-solid:wand-magic-sparkles")
                .componentLink("qwc-dev-services.js"));

        Map<String, DevServiceDescriptionBuildItem> combined = new TreeMap<>();
        addToMap(combined, devServiceDescriptions);
        addToMap(combined, otherDevServices);

        devServicesPages.addBuildTimeData("devServices", combined.values());

        return devServicesPages;

    }

    private void addToMap(Map<String, DevServiceDescriptionBuildItem> m, List<DevServiceDescriptionBuildItem> list) {
        if (!list.isEmpty()) {
            for (DevServiceDescriptionBuildItem i : list) {
                if (!m.containsKey(i.getName())) {
                    m.put(i.getName(), i);
                }
            }
        }
    }

    private List<DevServiceDescriptionBuildItem> getOtherDevServices(
            List<DevServicesResultBuildItem> devServicesResultBuildItems) {
        List<DevServiceDescriptionBuildItem> devServiceDescriptions = new ArrayList<>();
        for (DevServicesResultBuildItem devServicesResultBuildItem : devServicesResultBuildItems) {
            if (devServicesResultBuildItem.getContainerId() == null) {
                devServiceDescriptions.add(new DevServiceDescriptionBuildItem(devServicesResultBuildItem.getName(),
                        devServicesResultBuildItem.getDescription(),
                        null,
                        devServicesResultBuildItem.getConfig()));
            }
        }
        return devServiceDescriptions;
    }
}
