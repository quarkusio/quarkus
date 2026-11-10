package io.quarkus.reactive.pg.client.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbVersionBuildItem;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.spi.ReactivePoolBuildItem;
import io.quarkus.reactive.pg.client.runtime.DataSourcesReactivePostgreSQLConfig;
import io.quarkus.reactive.pg.client.runtime.PgPoolRecorder;
import io.quarkus.reactive.pg.client.runtime.PostgreSQLServiceBindingConverter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.pgclient.spi.PgDriver;
import io.vertx.sqlclient.Pool;

class ReactivePgClientProcessor {

    private static final String HEALTH_CHECK_SQL = "SELECT 1";
    public static final String TYPE = "PostgreSQL";

    @BuildStep
    NativeImageConfigBuildItem config() {
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.vertx.pgclient.impl.codec.StartupMessage")
                .addRuntimeInitializedClass("io.vertx.pgclient.impl.codec.DataTypeCodec")
                .build();
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.POSTGRESQL);
    }

    @BuildStep
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReactivePoolBuildItem> pools,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            List<DataSourceDefinedBuildItem> definedDataSources) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_PG_CLIENT));

        for (DataSourceDefinedBuildItem dataSource : definedDataSources) {
            if (!dataSource.getParadigms().contains(ProgrammingParadigm.REACTIVE)
                    || !DatabaseKind.isPostgreSQL(dataSource.getDbKind())) {
                continue;
            }
            String dataSourceName = dataSource.getName();

            serviceRegistrar.forService(Pool.class, dataSourceName)
                    .atPhase(Phase.DATA)
                    .require(DataSourcesRuntimeConfig.class)
                    .require(DataSourcesReactiveRuntimeConfig.class)
                    .require(DataSourcesReactivePostgreSQLConfig.class)
                    .require(Vertx.class)
                    .require(IntSupplier.class, "io.quarkus.vertx.event-loop-count")
                    .require(BeanContainer.class)
                    .request(TlsConfigurationRegistry.class)
                    .optional()
                    .onStart((ctx, runtimeConfig, reactiveRuntimeConfig, pgConfig, vertx, eventLoopCount, beanContainer,
                            tlsRegistry) -> {
                        Optional<Pool> pool = PgPoolRecorder.createPool(dataSourceName, runtimeConfig,
                                reactiveRuntimeConfig, pgConfig, vertx, eventLoopCount.getAsInt(),
                                tlsRegistry.orElse(null));
                        pool.ifPresent(p -> ctx.onStop(p::close));
                        return pool;
                    });
            pools.produce(new ReactivePoolBuildItem(dataSourceName, TYPE, HEALTH_CHECK_SQL));
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_PG_CLIENT));

        return new ServiceStartBuildItem("reactive-pg-client");
    }

    @BuildStep
    void registerDriver(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem("io.vertx.sqlclient.spi.Driver", PgDriver.class.getName()));
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind,
            BuildProducer<DefaultDataSourceDbVersionBuildItem> dbVersion) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            PostgreSQLServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.POSTGRESQL));
        dbVersion.produce(new DefaultDataSourceDbVersionBuildItem(DatabaseKind.POSTGRESQL,
                DatabaseVersionLoader.loadDefaultVersion("postgresql")));
    }
}
