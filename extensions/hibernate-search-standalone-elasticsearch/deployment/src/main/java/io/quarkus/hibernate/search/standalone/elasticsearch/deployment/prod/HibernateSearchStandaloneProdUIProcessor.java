package io.quarkus.hibernate.search.standalone.elasticsearch.deployment.prod;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.search.standalone.elasticsearch.deployment.HibernateSearchStandaloneEnabled;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.produi.HibernateSearchStandaloneProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page for standalone Hibernate Search. The Dev
 * UI component is not reused: it offers a mass-indexer / reindex action and
 * imports dev-only modules, so a bespoke read-only component is provided instead.
 */
@BuildSteps(onlyIf = HibernateSearchStandaloneEnabled.class)
public class HibernateSearchStandaloneProdUIProcessor {

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateSearchStandaloneProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Indexed Entity Types")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("pwc-hibernate-search-standalone.js"));
        return page;
    }
}
