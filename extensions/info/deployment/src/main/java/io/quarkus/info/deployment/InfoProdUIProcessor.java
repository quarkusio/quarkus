package io.quarkus.info.deployment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.info.deployment.spi.InfoBuildTimeContributorBuildItem;
import io.quarkus.info.deployment.spi.InfoBuildTimeValuesBuildItem;
import io.quarkus.info.runtime.InfoRecorder;
import io.quarkus.info.runtime.produi.InfoProdUIService;
import io.quarkus.info.runtime.spi.InfoContributor;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Contributes a read-only Prod UI page reusing the existing {@code qwc-info.js}
 * component. It serves the same aggregated info map exposed by {@code /q/info}
 * (OS, Java, build and git) via a runtime service, so no additional data beyond
 * the public info endpoint is exposed.
 */
public class InfoProdUIProcessor {

    @BuildStep(onlyIf = InfoEndpointEnabled.class)
    void createProdUIPage(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProducer,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        jsonRPCProducer.produce(new JsonRPCProvidersBuildItem(InfoProdUIService.class));

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Info")
                .icon("font-awesome-solid:circle-info")
                .componentLink("qwc-info.js"));
        prodUIProducer.produce(page);
    }

    @BuildStep(onlyIf = InfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeProdUIService(InfoRecorder recorder,
            List<InfoBuildTimeValuesBuildItem> buildTimeValues,
            List<InfoBuildTimeContributorBuildItem> contributors) {

        LinkedHashMap<String, Object> buildTimeInfo = new LinkedHashMap<>();
        for (var bi : buildTimeValues) {
            buildTimeInfo.put(bi.getName(), bi.getValue());
        }
        List<InfoContributor> infoContributors = contributors.stream()
                .map(InfoBuildTimeContributorBuildItem::getInfoContributor)
                .collect(Collectors.toList());

        RuntimeValue<Map<String, Object>> finalBuildInfo = recorder.getFinalInfo(buildTimeInfo, infoContributors);
        recorder.initializeProdUIService(finalBuildInfo);
    }
}
