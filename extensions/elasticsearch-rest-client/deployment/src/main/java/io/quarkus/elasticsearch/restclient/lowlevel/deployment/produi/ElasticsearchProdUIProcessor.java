package io.quarkus.elasticsearch.restclient.lowlevel.deployment.produi;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.produi.ElasticsearchProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page showing the Elasticsearch REST client
 * configuration (hosts, protocol, pool sizing, timeouts) and the live cluster
 * health. There is no Dev UI data page to reuse (the Elasticsearch Dev UI is
 * only a Dev Services console link), so a bespoke read-only component + service
 * is provided. The service issues only the read-only {@code _cluster/health}
 * query and never exposes the configured username, password or API key. The
 * {@code Rest5Client} bean is always present when the extension is in use, so the
 * page is contributed unconditionally.
 */
public class ElasticsearchProdUIProcessor {

    @BuildStep
    void createProdUIPage(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProducer,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {

        jsonRPCProducer.produce(new JsonRPCProvidersBuildItem(ElasticsearchProdUIService.class));

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Elasticsearch")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("pwc-elasticsearch.js"));
        prodUIProducer.produce(page);
    }
}
