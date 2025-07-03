package io.quarkus.devui.deployment.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.deployment.DevUIConfig;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates DevServices Page
 */
public class DevServicesProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    InternalPageBuildItem createDevServicesPages(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            List<DevServiceDescriptionBuildItem> devServiceDescriptions,
            List<DevServicesResultBuildItem> devServicesResultBuildItems, LaunchModeBuildItem launchModeBuildItem,
            DevUIConfig config) {

        List<DevServiceDescriptionBuildItem> otherDevServices = getOtherDevServices(devServicesResultBuildItems);

        InternalPageBuildItem devServicesPages = new InternalPageBuildItem("Dev Services", 40);

        devServicesPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Dev services")
                .icon("font-awesome-solid:wand-magic-sparkles")
                .componentLink("qwc-dev-services.js"));

        Collection<DevServiceDescriptionBuildItem> services = getServices(devServiceDescriptions, otherDevServices);

        devServicesPages.addBuildTimeData("devServices", services);

        if (launchModeBuildItem.getDevModeType().isPresent()
                && launchModeBuildItem.getDevModeType().get().equals(DevModeType.LOCAL)
                && config.allowExtensionManagement()) {

            BuildTimeActionBuildItem buildTimeActions = new BuildTimeActionBuildItem(NAMESPACE);
            getDevServices(buildTimeActions, devServiceDescriptions, otherDevServices);
            buildTimeActionProducer.produce(buildTimeActions);
        }

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
                        devServicesResultBuildItem.getConfig()));
            }
        }
        return devServiceDescriptions;
    }

    private void getDevServices(BuildTimeActionBuildItem buildTimeActions,
            List<DevServiceDescriptionBuildItem> devServiceDescriptions,
            List<DevServiceDescriptionBuildItem> otherDevServices) {
        buildTimeActions.addAction(new Object() {
        }.getClass().getEnclosingMethod().getName(),
                ignored -> CompletableFuture.supplyAsync(() -> getServices(devServiceDescriptions, otherDevServices)));
    }

    private Collection<DevServiceDescriptionBuildItem> getServices(List<DevServiceDescriptionBuildItem> devServiceDescriptions,
            List<DevServiceDescriptionBuildItem> otherDevServices) {
        Map<String, DevServiceDescriptionBuildItem> combined = new TreeMap<>();
        addToMap(combined, devServiceDescriptions);
        addToMap(combined, otherDevServices);

        return combined.values();
    }

    private static final String NAMESPACE = "devui-dev-services";

}
