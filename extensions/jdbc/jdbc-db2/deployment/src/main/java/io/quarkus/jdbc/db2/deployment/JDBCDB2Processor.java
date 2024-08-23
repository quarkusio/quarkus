package io.quarkus.jdbc.db2.deployment;

import com.ibm.db2.jcc.resources.ResourceKeys;
import com.ibm.db2.jcc.resources.Resources;
import com.ibm.db2.jcc.resources.SqljResources;
import com.ibm.db2.jcc.resources.T2uResourceKeys;
import com.ibm.db2.jcc.resources.T2uResources;
import com.ibm.db2.jcc.resources.T2zResourceKeys;
import com.ibm.db2.jcc.resources.T2zResources;
import com.ibm.db2.jcc.resources.T4ResourceKeys;
import com.ibm.db2.jcc.resources.T4Resources;

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
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jdbc.db2.runtime.DB2AgroalConnectionConfigurer;
import io.quarkus.jdbc.db2.runtime.DB2ServiceBindingConverter;

public class JDBCDB2Processor {

    private static final String DB2_DRIVER_CLASS = "com.ibm.db2.jcc.DB2Driver";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_DB2);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.DB2, DB2_DRIVER_CLASS,
                "com.ibm.db2.jcc.DB2XADataSource"));
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.jdbc(DatabaseKind.DB2);
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
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal,
        //for example when the driver is used with OpenTelemetry JDBC instrumentation.
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(DB2_DRIVER_CLASS).build());

        // register resource bundles for reflection (they are apparently classes...)
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Resources.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ResourceKeys.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(SqljResources.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(T2uResourceKeys.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(T2uResources.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(T2zResourceKeys.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(T2zResources.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(T4ResourceKeys.class).build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(T4Resources.class).build());
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

    @BuildStep
    void addExportsToNativeImage(BuildProducer<JPMSExportBuildItem> jpmsExports) {
        // com.ibm.db2:jcc:11.5.6.0 accesses sun.security.action.GetPropertyAction
        // which is strongly encapsulated in Java 17 requiring
        // --add-exports=java.base/sun.security.action=ALL-UNNAMED
        jpmsExports.produce(new JPMSExportBuildItem("java.base", "sun.security.action"));
    }
}
