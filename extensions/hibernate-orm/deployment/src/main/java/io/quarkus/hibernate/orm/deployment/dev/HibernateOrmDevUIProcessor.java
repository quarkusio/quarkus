package io.quarkus.hibernate.orm.deployment.dev;

import java.util.List;

import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfoCreateDDLSupplier;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevJsonRpcService;

@BuildSteps(onlyIf = { HibernateOrmEnabled.class, IsDevelopment.class })
public class HibernateOrmDevUIProcessor {

    @BuildStep
    public CardPageBuildItem create(HibernateOrmConfig config) {
        CardPageBuildItem card = new CardPageBuildItem();
        card.setLogo("hibernate_icon_dark.svg", "hibernate_icon_light.svg");
        card.addLibraryVersion("org.hibernate.orm", "hibernate-core", "Hibernate", "https://hibernate.org/orm/");
        card.addLibraryVersion("jakarta.persistence", "jakarta.persistence-api", "Jakarta Persistence",
                "https://jakarta.ee/specifications/persistence/");

        card.addPage(Page.webComponentPageBuilder()
                .title("Persistence Units")
                .componentLink("hibernate-orm-persistence-units.js")
                .icon("font-awesome-solid:boxes-stacked")
                .dynamicLabelJsonRPCMethodName("getNumberOfPersistenceUnits"));
        card.addPage(Page.webComponentPageBuilder()
                .title("Entity Types")
                .componentLink("hibernate-orm-entity-types.js")
                .icon("font-awesome-solid:table")
                .dynamicLabelJsonRPCMethodName("getNumberOfEntityTypes"));
        card.addPage(Page.webComponentPageBuilder()
                .title("Named Queries")
                .componentLink("hibernate-orm-named-queries.js")
                .icon("font-awesome-solid:circle-question")
                .dynamicLabelJsonRPCMethodName("getNumberOfNamedQueries"));
        card.addPage(Page.webComponentPageBuilder()
                .title("HQL Console")
                .componentLink("hibernate-orm-hql-console.js")
                .icon("font-awesome-solid:play")
                .metadata("allowHql", String.valueOf(config.devui().allowHql())));
        return card;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateOrmDevJsonRpcService.class);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem
                .builder()
                .addBeanClass(HibernateOrmDevJsonRpcService.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    void handleInitialSql(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<JdbcInitialSQLGeneratorBuildItem> initialSQLGeneratorBuildItemBuildProducer) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            String dsName = puDescriptor.getConfig().getDataSource().orElse(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
            initialSQLGeneratorBuildItemBuildProducer
                    .produce(new JdbcInitialSQLGeneratorBuildItem(dsName, new HibernateOrmDevInfoCreateDDLSupplier(puName)));
        }
    }

}
