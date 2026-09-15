package io.quarkus.hibernate.search.orm.elasticsearch.deployment.prod;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchEnabled;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.produi.HibernateSearchElasticsearchProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page for Hibernate Search. The Dev UI
 * component is not reused: it offers a mass-indexer / reindex action and imports
 * dev-only modules, so a bespoke read-only component is provided instead.
 */
@BuildSteps(onlyIf = HibernateSearchEnabled.class)
public class HibernateSearchElasticsearchProdUIProcessor {

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateSearchElasticsearchProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Indexed Entity Types")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("pwc-hibernate-search.js"));
        return page;
    }
}
