package io.quarkus.reactive.mysql.client.deployment;

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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.spi.ReactivePoolBuildItem;
import io.quarkus.reactive.mysql.client.runtime.DataSourcesReactiveMySQLConfig;
import io.quarkus.reactive.mysql.client.runtime.MySQLPoolRecorder;
import io.quarkus.reactive.mysql.client.runtime.MySQLServiceBindingConverter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.spi.MySQLDriver;
import io.vertx.sqlclient.Pool;

class ReactiveMySQLClientProcessor {

    private static final String HEALTH_CHECK_SQL = "SELECT 1";
    public static final String TYPE = "MySQL";

    @BuildStep
    ServiceStartBuildItem build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReactivePoolBuildItem> pools,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            List<DataSourceDefinedBuildItem> definedDataSources) {

        feature.produce(new FeatureBuildItem(Feature.REACTIVE_MYSQL_CLIENT));

        for (DataSourceDefinedBuildItem dataSource : definedDataSources) {
            if (!dataSource.getParadigms().contains(ProgrammingParadigm.REACTIVE)
                    || (!DatabaseKind.isMySQL(dataSource.getDbKind())
                            && !DatabaseKind.isMariaDB(dataSource.getDbKind()))) {
                continue;
            }
            String dataSourceName = dataSource.getName();

            serviceRegistrar.forService(Pool.class, dataSourceName)
                    .atPhase(Phase.DATA)
                    .require(DataSourcesRuntimeConfig.class)
                    .require(DataSourcesReactiveRuntimeConfig.class)
                    .require(DataSourcesReactiveMySQLConfig.class)
                    .require(Vertx.class)
                    .require(IntSupplier.class, "io.quarkus.vertx.event-loop-count")
                    .require(BeanContainer.class)
                    .request(TlsConfigurationRegistry.class)
                    .optional()
                    .onStart((ctx, runtimeConfig, reactiveRuntimeConfig, mysqlConfig, vertx, eventLoopCount, beanContainer,
                            tlsRegistry) -> {
                        Optional<Pool> pool = MySQLPoolRecorder.createPool(dataSourceName, runtimeConfig,
                                reactiveRuntimeConfig, mysqlConfig, vertx, eventLoopCount.getAsInt(),
                                tlsRegistry.orElse(null));
                        pool.ifPresent(p -> ctx.onStop(p::close));
                        return pool;
                    });
            pools.produce(new ReactivePoolBuildItem(dataSourceName, TYPE, HEALTH_CHECK_SQL));
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
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind,
            BuildProducer<DefaultDataSourceDbVersionBuildItem> dbVersion) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            MySQLServiceBindingConverter.class.getName()));
        }

        // Only producing a default db-kind for MySQL -- if we also produced one for MariaDB,
        // Quarkus would no longer be able to pick a default.
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.MYSQL));

        // Producing default db-versions for both MySQL and MariaDB,
        // so that explicit db-kind selection leads to the right default version.
        dbVersion.produce(new DefaultDataSourceDbVersionBuildItem(DatabaseKind.MYSQL,
                DatabaseVersionLoader.loadDefaultVersion("mysql")));
        dbVersion.produce(new DefaultDataSourceDbVersionBuildItem(DatabaseKind.MARIADB,
                DatabaseVersionLoader.loadDefaultVersion("mariadb")));
    }
}
