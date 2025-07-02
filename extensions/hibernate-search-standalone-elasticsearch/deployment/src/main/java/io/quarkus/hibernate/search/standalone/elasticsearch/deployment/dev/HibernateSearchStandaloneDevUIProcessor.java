package io.quarkus.hibernate.search.standalone.elasticsearch.deployment.dev;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.search.standalone.elasticsearch.deployment.HibernateSearchStandaloneEnabled;
import io.quarkus.hibernate.search.standalone.elasticsearch.deployment.HibernateSearchStandaloneEnabledBuildItem;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev.HibernateSearchStandaloneDevJsonRpcService;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev.HibernateSearchStandaloneDevRecorder;

@BuildSteps(onlyIf = { HibernateSearchStandaloneEnabled.class, IsDevelopment.class })
public class HibernateSearchStandaloneDevUIProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public CardPageBuildItem create(HibernateSearchStandaloneDevRecorder recorder,
            Optional<HibernateSearchStandaloneEnabledBuildItem> enabled) {
        recorder.initController(enabled.isPresent());

        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Indexed Entity Types")
                .componentLink("hibernate-search-standalone-elasticsearch-indexed-entity-types.js")
                .icon("font-awesome-solid:magnifying-glass")
                .dynamicLabelJsonRPCMethodName("getNumberOfIndexedEntityTypes"));

        return card;
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem
                .builder()
                .addBeanClass(HibernateSearchStandaloneDevJsonRpcService.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateSearchStandaloneDevJsonRpcService.class);
    }
}
