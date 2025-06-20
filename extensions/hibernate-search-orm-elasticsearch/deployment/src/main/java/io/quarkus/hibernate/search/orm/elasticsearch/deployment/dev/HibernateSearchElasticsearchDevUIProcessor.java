package io.quarkus.hibernate.search.orm.elasticsearch.deployment.dev;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchEnabled;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev.HibernateSearchElasticsearchDevJsonRpcService;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev.HibernateSearchElasticsearchDevRecorder;

@BuildSteps(onlyIf = { HibernateSearchEnabled.class, IsDevelopment.class })
public class HibernateSearchElasticsearchDevUIProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public CardPageBuildItem create(HibernateSearchElasticsearchDevRecorder recorder,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> persistenceUnitBuildItems) {
        Set<String> persistenceUnitNames = persistenceUnitBuildItems.stream()
                .map(HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem::getPersistenceUnitName)
                .collect(Collectors.toSet());
        recorder.initController(persistenceUnitNames);

        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Indexed Entity Types")
                .componentLink("hibernate-search-orm-elasticsearch-indexed-entity-types.js")
                .icon("font-awesome-solid:magnifying-glass")
                .dynamicLabelJsonRPCMethodName("getNumberOfIndexedEntityTypes"));

        return card;
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem
                .builder()
                .addBeanClass(HibernateSearchElasticsearchDevJsonRpcService.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateSearchElasticsearchDevJsonRpcService.class);
    }
}
