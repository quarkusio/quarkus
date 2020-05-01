package io.quarkus.hibernate.rx.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RxSessionFactoryRecorder {

    //    public RuntimeValue<RxSessionFactory> configurePgPool(
    //    		RuntimeValue<Vertx> vertx, 
    //    		BeanContainer container,
    //            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
    //            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
    //            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
    //            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
    //            LegacyDataSourceReactivePostgreSQLConfig legacyDataSourceReactivePostgreSQLConfig,
    //            boolean isLegacy,
    //            ShutdownContext shutdown) {
    //
    //        PgPool pgPool;
    //        if (!isLegacy) {
    //            pgPool = initialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
    //                    dataSourceReactiveRuntimeConfig,
    //                    dataSourceReactivePostgreSQLConfig);
    //        } else {
    //            pgPool = legacyInitialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
    //                    legacyDataSourcesRuntimeConfig.defaultDataSource, legacyDataSourceReactivePostgreSQLConfig);
    //        }
    //
    //        PgPoolProducer producer = container.instance(PgPoolProducer.class);
    //        producer.initialize(pgPool);
    //
    //        shutdown.addShutdownTask(pgPool::close);
    //        return new RuntimeValue<>(pgPool);
    //    }
}
