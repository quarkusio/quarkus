package io.quarkus.reactive.mysql.client.deployment;

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
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.mysql.client.runtime.DataSourceReactiveMySQLConfig;
import io.quarkus.reactive.mysql.client.runtime.LegacyDataSourceReactiveMySQLConfig;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolProducer;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mysqlclient.MySQLPool;

@SuppressWarnings("deprecation")
class ReactiveMySQLClientProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(MySQLPoolProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<MySQLPoolBuildItem> mysqlPool,
            BuildProducer<VertxPoolBuildItem> vertxPool,
            MySQLPoolRecorder recorder,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourceReactiveMySQLConfig legacyDataSourceReactiveMySQLConfig) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MYSQL_CLIENT));
        // Make sure the MySQLPoolProducer is initialized before the StartupEvent is fired
        ServiceStartBuildItem serviceStart = new ServiceStartBuildItem("reactive-mysql-client");

        // Note: we had to tweak that logic to support the legacy configuration
        if (dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent()
                && ((!DatabaseKind.isMySQL(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get())
                        && !DatabaseKind.isMariaDB(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get()))
                        || !dataSourceReactiveBuildTimeConfig.enabled)) {
            return serviceStart;
        }

        boolean isLegacy = !dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent();

        RuntimeValue<MySQLPool> mySqlPool = recorder.configureMySQLPool(vertx.getVertx(), beanContainer.getValue(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactiveMySQLConfig,
                legacyDataSourcesRuntimeConfig, legacyDataSourceReactiveMySQLConfig, isLegacy,
                shutdown);
        mysqlPool.produce(new MySQLPoolBuildItem(mySqlPool));

        boolean isDefault = true; // assume always the default pool for now
        vertxPool.produce(new VertxPoolBuildItem(mySqlPool, DatabaseKind.MYSQL, isDefault));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_MYSQL_CLIENT));

        return serviceStart;
    }

    @BuildStep
    HealthBuildItem addHealthCheck(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.reactive.mysql.client.runtime.health.ReactiveMySQLDataSourceHealthCheck",
                dataSourcesBuildTimeConfig.healthEnabled);
    }
}
