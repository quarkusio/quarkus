package io.quarkus.jdbc.oracle.deployment;

import static io.quarkus.deployment.Feature.JDBC_ORACLE;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jdbc.oracle.runtime.OracleAgroalConnectionConfigurer;
import io.quarkus.jdbc.oracle.runtime.OracleServiceBindingConverter;

/**
 * N.B. this processor is relatively simple as we rely on the /META-INF/native-image/
 * resources provided by the driver.
 * This should probably change, as we could probably generate better optimised
 * code by bypassing the static definitions coming with the driver.
 */
public class OracleProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(JDBC_ORACLE);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.ORACLE, "oracle.jdbc.driver.OracleDriver",
                "oracle.jdbc.xa.client.OracleXADataSource"));
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.jdbc(DatabaseKind.ORACLE);
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClass(OracleAgroalConnectionConfigurer.class)
                    .setDefaultScope(BuiltinScope.APPLICATION.getName())
                    .setUnremovable()
                    .build());
        }
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            OracleServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.ORACLE));
    }

}
