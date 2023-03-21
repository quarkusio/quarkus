package io.quarkus.hibernate.search.orm.elasticsearch.deployment.dev;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.deployment.HibernateSearchEnabled;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev.HibernateSearchElasticsearchDevInfoSupplier;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev.HibernateSearchElasticsearchDevRecorder;

@BuildSteps(onlyIf = { HibernateSearchEnabled.class, IsDevelopment.class })
@Deprecated // Only useful for the legacy Dev UI
public class HibernateSearchElasticsearchDevConsoleProcessor {

    @BuildStep
    public DevConsoleRuntimeTemplateInfoBuildItem exposeInfos(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("indexedPersistenceUnits",
                new HibernateSearchElasticsearchDevInfoSupplier(), this.getClass(), curateOutcomeBuildItem);
    }

    @BuildStep
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(HibernateSearchElasticsearchDevRecorder recorder) {
        return new DevConsoleRouteBuildItem("entity-types", "POST", recorder.indexEntity());
    }
}
