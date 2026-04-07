package io.quarkus.reactive.mysql.client.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveBuildTimeConfig;
import io.quarkus.reactive.datasource.spi.ReactivePoolBuildItem;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolRecorder;
import io.quarkus.reactive.mysql.client.runtime.MySQLServiceBindingConverter;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.mysqlclient.spi.MySQLDriver;
import io.vertx.sqlclient.Pool;

class ReactiveMySQLClientProcessor {

    private static final String HEALTH_CHECK_SQL = "SELECT 1";
    public static final String TYPE = "MySQL";

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReactivePoolBuildItem> pools,
            MySQLPoolRecorder recorder,
            VertxBuildItem vertx,
            EventLoopCountBuildItem eventLoopCount,
            ShutdownContextBuildItem shutdown,
            Optional<TlsRegistryBuildItem> tlsRegistryBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MYSQL_CLIENT));

        for (String dataSourceName : dataSourcesBuildTimeConfig.dataSources().keySet()) {
            if (!isReactiveMySQLPoolDefined(dataSourcesBuildTimeConfig, dataSourcesReactiveBuildTimeConfig, dataSourceName,
                    defaultDataSourceDbKindBuildItems, curateOutcomeBuildItem)) {
                continue;
            }

            Function<SyntheticCreationalContext<Pool>, Pool> poolFunction = recorder.configureMySQLPool(vertx.getVertx(),
                    eventLoopCount.getEventLoopCount(), dataSourceName, shutdown,
                    tlsRegistryBuildItem.map(TlsRegistryBuildItem::registry).orElse(null));
            pools.produce(new ReactivePoolBuildItem(dataSourceName, TYPE, poolFunction, HEALTH_CHECK_SQL));
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.REACTIVE_MYSQL_CLIENT));

        return new ServiceStartBuildItem("reactive-mysql-client");
    }

    @BuildStep
    List<DevServicesDatasourceConfigurationHandlerBuildItem> devDbHandler() {
        return List.of(DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.MYSQL),
                DevServicesDatasourceConfigurationHandlerBuildItem.reactive(DatabaseKind.MARIADB));
    }

    @BuildStep
    void registerDriver(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(new ServiceProviderBuildItem("io.vertx.sqlclient.spi.Driver", MySQLDriver.class.getName()));
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            MySQLServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.MYSQL));
    }

    private static boolean isReactiveMySQLPoolDefined(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesReactiveBuildTimeConfig dataSourcesReactiveBuildTimeConfig, String dataSourceName,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig
                .dataSources().get(dataSourceName);
        DataSourceReactiveBuildTimeConfig dataSourceReactiveBuildTimeConfig = dataSourcesReactiveBuildTimeConfig
                .dataSources().get(dataSourceName).reactive();

        Optional<String> dbKind = DefaultDataSourceDbKindBuildItem.resolve(dataSourceBuildTimeConfig.dbKind(),
                defaultDataSourceDbKindBuildItems,
                !io.quarkus.datasource.common.runtime.DataSourceUtil.isDefault(dataSourceName)
                        || dataSourceBuildTimeConfig.devservices().enabled()
                                .orElse(!dataSourcesBuildTimeConfig.hasNamedDataSources()),
                curateOutcomeBuildItem);

        if (!dbKind.isPresent()) {
            return false;
        }

        if ((!DatabaseKind.isMySQL(dbKind.get())
                && !DatabaseKind.isMariaDB(dbKind.get()))
                || !dataSourceReactiveBuildTimeConfig.enabled()) {
            return false;
        }

        return true;
    }
}
