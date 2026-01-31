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
            getDevServicesConfig(buildTimeActions, devServiceDescriptions, otherDevServices);
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
        buildTimeActions.actionBuilder()
                .methodName(new Object() {
                }.getClass().getEnclosingMethod().getName())
                .function(ignored -> CompletableFuture.supplyAsync(() -> getServices(devServiceDescriptions, otherDevServices)))
                .description(
                        "Get all the DevServices started by this Quarkus app, including information on container (if any) and the config that is being set automatically")
                .enableMcpFuctionByDefault()
                .build();
    }

    private void getDevServicesConfig(BuildTimeActionBuildItem buildTimeActions,
            List<DevServiceDescriptionBuildItem> devServiceDescriptions,
            List<DevServiceDescriptionBuildItem> otherDevServices) {
        buildTimeActions.actionBuilder()
                .methodName("devServicesConfig")
                .function(params -> CompletableFuture.supplyAsync(() -> getServices(devServiceDescriptions, otherDevServices)
                        .stream()
                        .filter(d -> d.getName().equals(params.get("name")))
                        .filter(d -> d.getConfigs() != null && !d.getConfigs().isEmpty())
                        .findFirst().map(d -> {
                            if (params.get("configKey") != null) {
                                String key = params.get("configKey");
                                return d.getConfigs().get(key);
                            } else {
                                return d.getConfigs();
                            }
                        }).orElse(null)))
                .description(
                        "Get the config or a specific config key for a DevService started by this Quarkus app")
                .parameter("name", String.class, "The name of the DevService", true)
                .parameter("configKey", String.class,
                        "The config key to get the value for. If not set, all the config is returned")
                .build();
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
