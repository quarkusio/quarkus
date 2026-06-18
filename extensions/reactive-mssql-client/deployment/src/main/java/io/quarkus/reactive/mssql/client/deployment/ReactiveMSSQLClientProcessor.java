package io.quarkus.reactive.mssql.client.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.reactive.datasource.spi.ReactivePoolBuildItem;
import io.quarkus.reactive.mssql.client.runtime.MSSQLPoolRecorder;
import io.quarkus.reactive.mssql.client.runtime.MsSQLServiceBindingConverter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mssqlclient.spi.MSSQLDriver;
import io.vertx.sqlclient.Pool;

class ReactiveMSSQLClientProcessor {

    private static final String HEALTH_CHECK_SQL = "SELECT 1";
    public static final String TYPE = "MSSQL";

    @BuildStep
    NativeImageConfigBuildItem config() {
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.vertx.mssqlclient.impl.codec.DataType")
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReactivePoolBuildItem> pools,
            MSSQLPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            Optional<TlsRegistryBuildItem> tlsRegistryBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            List<DataSourceDefinedBuildItem> definedDataSources) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MSSQL_CLIENT));

        for (DataSourceDefinedBuildItem dataSource : definedDataSources) {
            if (!dataSource.getParadigms().contains(ProgrammingParadigm.REACTIVE)
                    || !DatabaseKind.isMsSQL(dataSource.getDbKind())) {
                continue;
            }
            String dataSourceName = dataSource.getName();

            Function<SyntheticCreationalContext<Pool>, Pool> poolFunction = recorder.configureMSSQLPool(vertx.getVertx(),
                    eventLoopCount.getEventLoopCount(), dataSourceName, shutdown,
                    tlsRegistryBuildItem.map(TlsRegistryBuildItem::registry).orElse(null));
            pools.produce(new ReactivePoolBuildItem(dataSourceName, TYPE, poolFunction, HEALTH_CHECK_SQL));
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_MSSQL_CLIENT));

        return new ServiceStartBuildItem("reactive-mssql-client");
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.MSSQL);
    }

    @BuildStep
    void registerDriver(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem("io.vertx.sqlclient.spi.Driver", MSSQLDriver.class.getName()));
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            MsSQLServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.MSSQL));
    }
}
