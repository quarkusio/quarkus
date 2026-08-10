package io.quarkus.hibernate.orm.deployment.prod;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.runtime.produi.HibernateOrmProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page for Hibernate ORM. The Dev UI component
 * is not reused: it embeds an HQL console and DDL create / drop scripts and
 * imports dev-only modules, so a bespoke read-only component is provided instead.
 */
@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public class HibernateOrmProdUIProcessor {

    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateOrmProdUIService.class);
    }

    @BuildStep
    ProdUIPageBuildItem createProdUI() {
        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Persistence Units")
                .icon("font-awesome-solid:boxes-stacked")
                .componentLink("pwc-hibernate-orm.js"));
        return page;
    }
}
