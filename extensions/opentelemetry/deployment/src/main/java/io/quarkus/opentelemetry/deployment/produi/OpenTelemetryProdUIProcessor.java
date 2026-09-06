package io.quarkus.opentelemetry.deployment.produi;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.produi.OpenTelemetryProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page showing the OpenTelemetry exporter/sampler
 * configuration and span export status. There is no Dev UI data page to reuse
 * (the OpenTelemetry Dev UI ships only i18n bundles), so a bespoke read-only
 * component + service is provided. The service reads only configuration - never
 * the OTLP headers, key/cert or trust-cert - and issues no requests. The page is
 * only contributed when OpenTelemetry is enabled.
 */
@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class OpenTelemetryProdUIProcessor {

    @BuildStep
    void createProdUIPage(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProducer,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {

        jsonRPCProducer.produce(new JsonRPCProvidersBuildItem(OpenTelemetryProdUIService.class));

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        // The cards page summarises the configuration; the raw table is the companion lookup.
        page.addPage(Page.webComponentPageBuilder()
                .title("OpenTelemetry")
                .icon("font-awesome-solid:diagram-project")
                .componentLink("pwc-opentelemetry.js"));
        page.addPage(Page.webComponentPageBuilder()
                .title("Raw data")
                .icon("font-awesome-solid:table")
                .componentLink("pwc-opentelemetry-raw.js"));
        prodUIProducer.produce(page);
    }
}
