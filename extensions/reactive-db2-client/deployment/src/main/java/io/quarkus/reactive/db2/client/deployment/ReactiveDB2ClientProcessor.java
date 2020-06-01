package io.quarkus.reactive.db2.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.db2.client.runtime.DB2PoolProducer;
import io.quarkus.reactive.db2.client.runtime.DB2PoolRecorder;
import io.quarkus.reactive.db2.client.runtime.DataSourceReactiveDB2Config;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

class ReactiveDB2ClientProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(DB2PoolProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature, BuildProducer<DB2PoolBuildItem> db2Pool,
            DB2PoolRecorder recorder,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, ShutdownContextBuildItem shutdown,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REACTIVE_DB2_CLIENT));
        // Make sure the DB2PoolProducer is initialized before the StartupEvent is fired
        ServiceStartBuildItem serviceStart = new ServiceStartBuildItem("reactive-db2-client");

        // Note: we had to tweak that logic to support the legacy configuration
        if (dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent()
                && (!DatabaseKind.isDB2(dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get())
                        || !dataSourceReactiveBuildTimeConfig.enabled)) {
            return serviceStart;
        }

        db2Pool.produce(new DB2PoolBuildItem(recorder.configureDB2Pool(vertx.getVertx(), beanContainer.getValue(),
                dataSourcesRuntimeConfig, dataSourceReactiveRuntimeConfig, dataSourceReactiveDB2Config,
                shutdown)));

        return serviceStart;
    }

    @BuildStep
    HealthBuildItem addHealthCheck(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.reactive.db2.client.runtime.health.ReactiveDB2DataSourceHealthCheck",
                dataSourcesBuildTimeConfig.healthEnabled, "datasource");
    }
}
