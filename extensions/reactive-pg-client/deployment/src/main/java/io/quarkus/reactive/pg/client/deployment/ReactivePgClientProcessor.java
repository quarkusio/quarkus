package io.quarkus.reactive.pg.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourcesRuntimeConfig;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.pg.client.runtime.DataSourceReactivePostgreSQLConfig;
import io.quarkus.reactive.pg.client.runtime.LegacyDataSourceReactivePostgreSQLConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolProducer;
import io.quarkus.reactive.pg.client.runtime.PgPoolRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.pgclient.PgPool;

@SuppressWarnings("deprecation")
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
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<PgPoolBuildItem> pgPool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            PgPoolRecorder recorder,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourceReactivePostgreSQLConfig legacyDataSourceReactivePostgreSQLConfig) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_PG_CLIENT));
        // Make sure the PgPoolProducer is initialized before the StartupEvent is fired
        ServiceStartBuildItem serviceStart = new ServiceStartBuildItem("reactive-pg-client");

        // Note: we had to tweak that logic to support the legacy configuration
        if (dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent()
                && (!DatabaseKind.isPostgreSQL(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get())
                        || !dataSourceReactiveBuildTimeConfig.enabled)) {
            return serviceStart;
        }

        boolean isLegacy = !dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent();

        RuntimeValue<PgPool> pool = recorder.configurePgPool(vertx.getVertx(), beanContainer.getValue(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactivePostgreSQLConfig,
                legacyDataSourcesRuntimeConfig, legacyDataSourceReactivePostgreSQLConfig, isLegacy,
                shutdown);
        pgPool.produce(new PgPoolBuildItem(pool));

        boolean isDefault = true; // assume always the default pool for now
        vertxPool.produce(new VertxPoolBuildItem(pool, DatabaseKind.POSTGRESQL, isDefault));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_PG_CLIENT));

        return serviceStart;
    }

    @BuildStep
    HealthBuildItem addHealthCheck(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.reactive.pg.client.runtime.health.ReactivePgDataSourceHealthCheck",
                dataSourcesBuildTimeConfig.healthEnabled);
    }
}
