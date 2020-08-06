package io.quarkus.jdbc.mssql.deployment;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.jdbc.mssql.runtime.MsSQLAgroalConnectionConfigurer;

public class MsSQLProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_MSSQL);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.MSSQL, "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "com.microsoft.sqlserver.jdbc.SQLServerXADataSource"));
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClass(MsSQLAgroalConnectionConfigurer.class)
                    .setDefaultScope(BuiltinScope.APPLICATION.getName())
                    .setUnremovable()
                    .build());
        }
    }

    @BuildStep
    void nativeResources(BuildProducer<NativeImageResourceBundleBuildItem> resources,
            BuildProducer<NativeImageEnableAllCharsetsBuildItem> nativeEnableAllCharsets) {
        resources.produce(new NativeImageResourceBundleBuildItem("com.microsoft.sqlserver.jdbc.SQLServerResource"));
        nativeEnableAllCharsets.produce(new NativeImageEnableAllCharsetsBuildItem());
    }

    @BuildStep
    public RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        return new RuntimeInitializedClassBuildItem("com.microsoft.sqlserver.jdbc.KerbAuthentication");
    }
}
