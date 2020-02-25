package io.quarkus.reactive.pg.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.pg.client.runtime.DataSourceReactivePostgreSQLConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolProducer;
import io.quarkus.reactive.pg.client.runtime.PgPoolRecorder;
import io.quarkus.vertx.deployment.VertxBuildItem;

class ReactivePgClientProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(PgPoolProducer.class);
    }

    @BuildStep
    NativeImageConfigBuildItem config() {
        return NativeImageConfigBuildItem.builder().addRuntimeInitializedClass("io.vertx.pgclient.impl.codec.StartupMessage")
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(BuildProducer<FeatureBuildItem> feature, BuildProducer<PgPoolBuildItem> pgPool, PgPoolRecorder recorder,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REACTIVE_PG_CLIENT));

        if (!dataSourcesBuildTimeConfig.defaultDataSource.kind.isPresent()
                || !"postgresql".equals(dataSourcesBuildTimeConfig.defaultDataSource.kind.get())
                || !dataSourceReactiveBuildTimeConfig.enabled) {
            return;
        }

        pgPool.produce(new PgPoolBuildItem(recorder.configurePgPool(vertx.getVertx(), beanContainer.getValue(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactivePostgreSQLConfig,
                shutdown)));
    }
}
