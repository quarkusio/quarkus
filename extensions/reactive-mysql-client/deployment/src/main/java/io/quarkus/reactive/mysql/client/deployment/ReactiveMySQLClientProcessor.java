package io.quarkus.reactive.mysql.client.deployment;

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
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.mysql.client.runtime.DataSourceReactiveMySQLConfig;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolProducer;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolRecorder;
import io.quarkus.vertx.deployment.VertxBuildItem;

class ReactiveMySQLClientProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(MySQLPoolProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(BuildProducer<FeatureBuildItem> feature, BuildProducer<MySQLPoolBuildItem> mysqlPool, MySQLPoolRecorder recorder,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REACTIVE_MYSQL_CLIENT));

        if (!dataSourcesBuildTimeConfig.defaultDataSource.kind.isPresent()
                || (!"mysql".equals(dataSourcesBuildTimeConfig.defaultDataSource.kind.get())
                        && !"mariadb".equals(dataSourcesBuildTimeConfig.defaultDataSource.kind.get()))
                || !dataSourceReactiveBuildTimeConfig.enabled) {
            return;
        }

        mysqlPool.produce(new MySQLPoolBuildItem(recorder.configureMySQLPool(vertx.getVertx(), beanContainer.getValue(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactiveMySQLConfig,
                shutdown)));
    }
}
