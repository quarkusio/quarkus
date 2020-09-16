package io.quarkus.reactive.mysql.client.deployment;

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
import io.quarkus.reactive.mysql.client.runtime.DataSourceReactiveMySQLConfig;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolProducer;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Pool;

class ReactiveMySQLClientProcessor {

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
                .addBeanClass(MySQLPoolProducer.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<MySQLPoolBuildItem> mysqlPool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            MySQLPoolRecorder recorder,
            VertxBuildItem vertx,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans, ShutdownContextBuildItem shutdown,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MYSQL_CLIENT));
        // Make sure the MySQLPoolProducer is initialized before the StartupEvent is fired
        ServiceStartBuildItem serviceStart = new ServiceStartBuildItem("reactive-mysql-client");

        if (!createPool(dataSourcesBuildTimeConfig, dataSourceReactiveBuildTimeConfig)) {
            return serviceStart;
        }

        RuntimeValue<MySQLPool> mySqlPool = recorder.configureMySQLPool(vertx.getVertx(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactiveMySQLConfig,
                shutdown);
        mysqlPool.produce(new MySQLPoolBuildItem(mySqlPool));

        // Synthetic bean for MySQLPool
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(MySQLPool.class).addType(Pool.class).scope(Singleton.class)
                .runtimeValue(mySqlPool)
                .setRuntimeInit().done());

        boolean isDefault = true; // assume always the default pool for now
        vertxPool.produce(new VertxPoolBuildItem(mySqlPool, DatabaseKind.MYSQL, isDefault));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_MYSQL_CLIENT));

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

        healthChecks.produce(
                new HealthBuildItem("io.quarkus.reactive.mysql.client.runtime.health.ReactiveMySQLDataSourceHealthCheck",
                        dataSourcesBuildTimeConfig.healthEnabled));
    }

    private static boolean createPool(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig) {
        if (!dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent()) {
            return false;
        }

        if ((!DatabaseKind.isMySQL(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get())
                && !DatabaseKind.isMariaDB(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get()))
                || !dataSourceReactiveBuildTimeConfig.enabled) {
            return false;
        }

        return true;
    }
}
