package io.quarkus.reactive.db2.client.deployment;

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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.reactive.datasource.spi.ReactivePoolBuildItem;
import io.quarkus.reactive.db2.client.runtime.DB2PoolRecorder;
import io.quarkus.reactive.db2.client.runtime.DB2ServiceBindingConverter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.db2client.spi.DB2Driver;
import io.vertx.sqlclient.Pool;

class ReactiveDB2ClientProcessor {

    private static final String HEALTH_CHECK_SQL = "SELECT 1 FROM SYSIBM.SYSDUMMY1";
    public static final String TYPE = "DB2";

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReactivePoolBuildItem> pools,
            DB2PoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            Optional<TlsRegistryBuildItem> tlsRegistryBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            List<DataSourceDefinedBuildItem> definedDataSources) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_DB2_CLIENT));

        for (DataSourceDefinedBuildItem dataSource : definedDataSources) {
            if (!dataSource.getParadigms().contains(ProgrammingParadigm.REACTIVE)
                    || !DatabaseKind.isDB2(dataSource.getDbKind())) {
                continue;
            }
            String dataSourceName = dataSource.getName();

            Function<SyntheticCreationalContext<Pool>, Pool> poolFunction = recorder.configureDB2Pool(vertx.getVertx(),
                    eventLoopCount.getEventLoopCount(), dataSourceName, shutdown,
                    tlsRegistryBuildItem.map(TlsRegistryBuildItem::registry).orElse(null));
            pools.produce(new ReactivePoolBuildItem(dataSourceName, TYPE, poolFunction, HEALTH_CHECK_SQL));
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_DB2_CLIENT));

        return new ServiceStartBuildItem("reactive-db2-client");
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.DB2);
    }

    @BuildStep
    void registerDriver(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem("io.vertx.sqlclient.spi.Driver", DB2Driver.class.getName()));
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            DB2ServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.DB2));
    }
}
