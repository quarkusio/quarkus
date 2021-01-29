package io.quarkus.jdbc.db2.deployment;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jdbc.db2.runtime.DB2AgroalConnectionConfigurer;
import io.quarkus.jdbc.db2.runtime.DB2ServiceBindingConverter;

public class JDBCDB2Processor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_DB2);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.DB2, "com.ibm.db2.jcc.DB2Driver",
                "com.ibm.db2.jcc.DB2XADataSource"));
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans
                    .produce(new AdditionalBeanBuildItem.Builder().addBeanClass(DB2AgroalConnectionConfigurer.class)
                            .setDefaultScope(BuiltinScope.APPLICATION.getName())
                            .setUnremovable()
                            .build());
        }
    }

    @BuildStep
    NativeImageConfigBuildItem build() {
        // The DB2 JDBC driver has been updated with conditional checks for the
        // "QuarkusWithJcc" system property which will no-op some code paths that
        // are not needed for T4 JDBC usage and are incompatible with native mode
        return NativeImageConfigBuildItem.builder()
                .addNativeImageSystemProperty("QuarkusWithJcc", "true")
                .build();
    }

    @BuildStep
    NativeImageEnableAllCharsetsBuildItem enableAllCharsets() {
        // When connecting to DB2 on z/OS the Cp037 charset is required
        return new NativeImageEnableAllCharsetsBuildItem();
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            DB2ServiceBindingConverter.class.getName()));
        }
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.DB2));
    }
}
