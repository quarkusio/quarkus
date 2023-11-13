package io.quarkus.devui.deployment.menu;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.DevUIRecorder;
import io.quarkus.devui.runtime.EndpointInfo;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;

/**
 * This creates Endpoints Page
 */
public class EndpointsProcessor {
    private static final String DEVUI = "dev-ui";

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addEndpointInfos(List<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            DevUIRecorder recorder, HttpRootPathBuildItem httpRoot) {

        List<EndpointInfo> endpoints = displayableEndpoints
                .stream()
                .map(v -> new EndpointInfo(v.getEndpoint(httpRoot), v.getDescription()))
                .sorted()
                .collect(Collectors.toList());

        recorder.setEndpoints(endpoints);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createEndpointsPage(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        String basepath = nonApplicationRootPathBuildItem.resolvePath(DEVUI);

        InternalPageBuildItem endpointsPage = new InternalPageBuildItem("Endpoints", 25);

        endpointsPage.addBuildTimeData("basepath", basepath);

        // Page
        endpointsPage.addPage(Page.webComponentPageBuilder()
                .namespace("devui-endpoints")
                .title("Endpoints")
                .icon("font-awesome-solid:plug")
                .componentLink("qwc-endpoints.js"));

        return endpointsPage;
    }
}
