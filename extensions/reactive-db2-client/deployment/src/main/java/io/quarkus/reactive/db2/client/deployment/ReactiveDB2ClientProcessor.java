package io.quarkus.reactive.db2.client.deployment;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
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
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.db2.client.runtime.DB2PoolRecorder;
import io.quarkus.reactive.db2.client.runtime.DataSourcesReactiveDB2Config;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.Pool;

class ReactiveDB2ClientProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DB2PoolBuildItem> db2Pool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            DB2PoolRecorder recorder,
            VertxBuildItem vertx,
            ShutdownContextBuildItem shutdown,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveDB2Config dataSourcesReactiveDB2Config) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_DB2_CLIENT));

        createPoolIfDefined(recorder, vertx, shutdown, db2Pool, vertxPool, syntheticBeans,
                DataSourceUtil.DEFAULT_DATASOURCE_NAME, dataSourcesBuildTimeConfig,
                dataSourcesRuntimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourcesReactiveRuntimeConfig,
                dataSourcesReactiveDB2Config);

        for (String dataSourceName : dataSourcesBuildTimeConfig.namedDataSources.keySet()) {
            createPoolIfDefined(recorder, vertx, shutdown, db2Pool, vertxPool, syntheticBeans, dataSourceName,
                    dataSourcesBuildTimeConfig, dataSourcesRuntimeConfig, dataSourcesReactiveBuildTimeConfig,
                    dataSourcesReactiveRuntimeConfig, dataSourcesReactiveDB2Config);
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_DB2_CLIENT));

        return new ServiceStartBuildItem("reactive-db2-client");
    }

    /**
     * The health check needs to be produced in a separate method to avoid a circular dependency (the Vert.x instance creation
     * consumes the AdditionalBeanBuildItems).
     */
    @BuildStep
    void addHealthCheck(
            BuildProducer<HealthBuildItem> healthChecks,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig) {
        if (!hasPools(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig)) {
            return;
        }

        healthChecks.produce(
                new HealthBuildItem("io.quarkus.reactive.db2.client.runtime.health.ReactiveDB2DataSourcesHealthCheck",
                        dataSourcesBuildTimeConfig.healthEnabled));
    }

    private void createPoolIfDefined(DB2PoolRecorder recorder,
            VertxBuildItem vertx,
            ShutdownContextBuildItem shutdown,
            BuildProducer<DB2PoolBuildItem> db2Pool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            String dataSourceName,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveDB2Config dataSourcesReactiveDB2Config) {

        if (!isReactiveDB2PoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourceName)) {
            return;
        }

        RuntimeValue<DB2Pool> pool = recorder.configureDB2Pool(vertx.getVertx(),
                dataSourceName,
                dataSourcesRuntimeConfig,
                dataSourcesReactiveRuntimeConfig,
                dataSourcesReactiveDB2Config,
                shutdown);
        db2Pool.produce(new DB2PoolBuildItem(dataSourceName, pool));

        ExtendedBeanConfigurator db2PoolBeanConfigurator = SyntheticBeanBuildItem.configure(DB2Pool.class)
                .defaultBean()
                .addType(Pool.class)
                .scope(ApplicationScoped.class)
                .runtimeValue(pool)
                .unremovable()
                .setRuntimeInit();

        addQualifiers(db2PoolBeanConfigurator, dataSourceName);

        syntheticBeans.produce(db2PoolBeanConfigurator.done());

        ExtendedBeanConfigurator mutinyDB2PoolConfigurator = SyntheticBeanBuildItem
                .configure(io.vertx.mutiny.db2client.DB2Pool.class)
                .defaultBean()
                .scope(ApplicationScoped.class)
                .runtimeValue(recorder.mutinyDB2Pool(pool))
                .setRuntimeInit();

        addQualifiers(mutinyDB2PoolConfigurator, dataSourceName);

        syntheticBeans.produce(mutinyDB2PoolConfigurator.done());

        vertxPool.produce(new VertxPoolBuildItem(pool, DatabaseKind.DB2, DataSourceUtil.isDefault(dataSourceName)));
    }

    private static boolean isReactiveDB2PoolDefined(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig, String dataSourceName) {
        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig
                .getDataSourceRuntimeConfig(dataSourceName);
        DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                .getDataSourceReactiveBuildTimeConfig(dataSourceName);

        if (!dataSourceBuildTimeConfig.dbKind.isPresent()) {
            return false;
        }

        if (!DatabaseKind.isDB2(dataSourceBuildTimeConfig.dbKind.get())
                || !dataSourceReactiveBuildTimeConfig.enabled) {
            return false;
        }

        return true;
    }

    private boolean hasPools(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig) {
        if (isReactiveDB2PoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig,
                DataSourceUtil.DEFAULT_DATASOURCE_NAME)) {
            return true;
        }

        for (String dataSourceName : dataSourcesBuildTimeConfig.namedDataSources.keySet()) {
            if (isReactiveDB2PoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig,
                    dataSourceName)) {
                return true;
            }
        }

        return false;
    }

    private static void addQualifiers(ExtendedBeanConfigurator configurator, String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            configurator.addQualifier(DotNames.DEFAULT);
        } else {
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", dataSourceName).done();
            configurator.addQualifier().annotation(ReactiveDataSource.class).addValue("value", dataSourceName)
                    .done();
        }
    }
}
