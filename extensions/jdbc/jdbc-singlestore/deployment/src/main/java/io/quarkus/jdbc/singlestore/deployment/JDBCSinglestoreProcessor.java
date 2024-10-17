package io.quarkus.jdbc.singlestore.deployment;

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
import io.quarkus.jdbc.singlestore.runtime.SinglestoreAgroalConnectionConfigurer;
import io.quarkus.jdbc.singlestore.runtime.SinglestoreServiceBindingConverter;

public class JDBCSinglestoreProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_SINGLESTORE);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver, BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        jdbcDriver.produce(
                new JdbcDriverBuildItem(DatabaseKind.SINGLESTORE, "com.singlestore.jdbc.Driver",
                        "com.singlestore.jdbc.SinglestoreDataSource"));

        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.SINGLESTORE));
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.jdbc(DatabaseKind.SINGLESTORE);
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans
                    .produce(new AdditionalBeanBuildItem.Builder().addBeanClass(SinglestoreAgroalConnectionConfigurer.class)
                            .setDefaultScope(BuiltinScope.APPLICATION.getName())
                            .setUnremovable()
                            .build());
        }
    }

    @BuildStep
    void registerAuthenticationPlugins(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        // make sure that all plugins are available
        serviceProvider
                .produce(
                        ServiceProviderBuildItem.allProvidersFromClassPath("com.singlestore.jdbc.plugin.AuthenticationPlugin"));
    }

    @BuildStep
    void registerCodecs(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider
                .produce(ServiceProviderBuildItem.allProvidersFromClassPath("com.singlestore.jdbc.plugin.Codec"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void addNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resources) {
        // singlestore.properties is used by com.singlestore.jdbc.util.VersionFactory and is small enough.
        // driver.properties is not added because it only provides optional descriptions for
        // com.singlestore.jdbc.Driver.getPropertyInfo(), which is probably not even called.
        resources.produce(new NativeImageResourceBuildItem("singlestore-jdbc-client.properties"));

        // necessary when jdbcUrl contains useSsl=true
        resources.produce(new NativeImageResourceBuildItem("deprecated.properties"));
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            SinglestoreServiceBindingConverter.class.getName()));
        }
    }
}
