package io.quarkus.reactive.db2.client.deployment;

import javax.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.db2.client.runtime.DB2PoolProducer;
import io.quarkus.reactive.db2.client.runtime.DB2PoolRecorder;
import io.quarkus.reactive.db2.client.runtime.DataSourceReactiveDB2Config;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.Pool;

class ReactiveDB2ClientProcessor {

    /**
     * The producer needs to be produced in a separate method to avoid a circular dependency (the Vert.x instance creation
     * consumes the AdditionalBeanBuildItems).
     */
    @BuildStep
    void poolProducer(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig) {
        if (!createPool(dataSourcesBuildTimeConfig, dataSourceReactiveBuildTimeConfig)) {
            return;
        }

        additionalBeans.produce(new AdditionalBeanBuildItem.Builder()
                .addBeanClass(DB2PoolProducer.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DB2PoolBuildItem> db2Pool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            DB2PoolRecorder recorder,
            VertxBuildItem vertx,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans, ShutdownContextBuildItem shutdown,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_DB2_CLIENT));
        // Make sure the DB2PoolProducer is initialized before the StartupEvent is fired
        ServiceStartBuildItem serviceStart = new ServiceStartBuildItem("reactive-db2-client");

        if (!createPool(dataSourcesBuildTimeConfig, dataSourceReactiveBuildTimeConfig)) {
            return serviceStart;
        }

        RuntimeValue<DB2Pool> db2PoolValue = recorder.configureDB2Pool(vertx.getVertx(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactiveDB2Config,
                shutdown);
        db2Pool.produce(new DB2PoolBuildItem(db2PoolValue));

        // Synthetic bean for DB2Pool
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(DB2Pool.class).addType(Pool.class).scope(Singleton.class)
                .runtimeValue(db2PoolValue)
                .setRuntimeInit().done());

        boolean isDefault = true; // assume always the default pool for now
        vertxPool.produce(new VertxPoolBuildItem(db2PoolValue, DatabaseKind.DB2, isDefault));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_DB2_CLIENT));

        return serviceStart;
    }

    @BuildStep
    void addHealthCheck(
            BuildProducer<HealthBuildItem> healthChecks,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig) {
        if (!createPool(dataSourcesBuildTimeConfig, dataSourceReactiveBuildTimeConfig)) {
            return;
        }

        healthChecks
                .produce(new HealthBuildItem("io.quarkus.reactive.db2.client.runtime.health.ReactiveDB2DataSourceHealthCheck",
                        dataSourcesBuildTimeConfig.healthEnabled));
    }

    private static boolean createPool(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig) {
        if (!dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent()) {
            return false;
        }

        if (!DatabaseKind.isDB2(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get())
                || !dataSourceReactiveBuildTimeConfig.enabled) {
            return false;
        }

        return true;
    }
}
