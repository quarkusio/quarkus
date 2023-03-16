package io.quarkus.hibernate.search.orm.elasticsearch.deployment.dev;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchEnabled;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev.HibernateSearchElasticsearchDevRecorder;

@BuildSteps(onlyIf = { HibernateSearchEnabled.class, IsDevelopment.class })
public class HibernateSearchElasticsearchDevConsoleProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public DevConsoleRuntimeTemplateInfoBuildItem collectBeanInfo(HibernateSearchElasticsearchDevRecorder recorder,
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> persistenceUnitBuildItems) {
        Set<String> persistenceUnitNames = persistenceUnitBuildItems.stream()
                .map(HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem::getPersistenceUnitName)
                .collect(Collectors.toSet());
        return new DevConsoleRuntimeTemplateInfoBuildItem("indexedPersistenceUnits",
                recorder.infoSupplier(runtimeConfig, persistenceUnitNames), this.getClass(), curateOutcomeBuildItem);
    }

    @BuildStep
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(HibernateSearchElasticsearchDevRecorder recorder) {
        return new DevConsoleRouteBuildItem("entity-types", "POST", recorder.indexEntity());
    }
}
