package io.quarkus.jdbc.mariadb.deployment;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.jdbc.mariadb.runtime.MariaDBAgroalConnectionConfigurer;
import io.quarkus.jdbc.mariadb.runtime.MariaDBServiceBindingConverter;

public class JDBCMariaDBProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_MARIADB);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.MARIADB, "org.mariadb.jdbc.Driver",
                "org.mariadb.jdbc.MariaDbDataSource"));

        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.MARIADB));
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.jdbc(DatabaseKind.MARIADB);
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans
                    .produce(new AdditionalBeanBuildItem.Builder().addBeanClass(MariaDBAgroalConnectionConfigurer.class)
                            .setDefaultScope(BuiltinScope.APPLICATION.getName()).setUnremovable().build());
        }
    }

    @BuildStep
    void registerAuthenticationPlugins(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        // make sure that all plugins are available
        serviceProvider.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("org.mariadb.jdbc.plugin.AuthenticationPlugin"));
    }

    @BuildStep
    void registerCodecs(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath("org.mariadb.jdbc.plugin.Codec"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void addNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resources) {
        // mariadb.properties is used by org.mariadb.jdbc.util.VersionFactory and is small enough.
        // driver.properties is not added because it only provides optional descriptions for
        // org.mariadb.jdbc.Driver.getPropertyInfo(), which is probably not even called.
        resources.produce(new NativeImageResourceBuildItem("mariadb.properties"));

        // necessary when jdbcUrl contains useSsl=true
        resources.produce(new NativeImageResourceBuildItem("deprecated.properties"));
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(new ServiceProviderBuildItem(
                    "io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                    MariaDBServiceBindingConverter.class.getName()));
        }
    }
}
