package io.quarkus.jdbc.hsqldb.deployment;

import java.util.Set;

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
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageEnableModule;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.jdbc.hsqldb.runtime.HsqldbAgroalConnectionConfigurer;
import io.quarkus.maven.dependency.ArtifactKey;

public class JdbcHsqldbProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JDBC_HSQLDB);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver
                .produce(new JdbcDriverBuildItem(DatabaseKind.HSQLDB, "org.hsqldb.jdbc.JDBCDriver"));
    }

    @BuildStep
    DevServicesDatasourceConfigurationHandlerBuildItem devDbHandler() {
        return DevServicesDatasourceConfigurationHandlerBuildItem.jdbc(DatabaseKind.HSQLDB);
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClass(HsqldbAgroalConnectionConfigurer.class)
                    .setDefaultScope(BuiltinScope.APPLICATION.getName())
                    .setUnremovable()
                    .build());
        }
    }

    @BuildStep
    void registerDefaultDbType(BuildProducer<DefaultDataSourceDbKindBuildItem> dbKind) {
        dbKind.produce(new DefaultDataSourceDbKindBuildItem(DatabaseKind.HSQLDB));
    }

    /*
    TODO: These need analysis of the HSQLDB sources.

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("org.h2.store.fs.niomem.FileNioMemData"));
    }

    @BuildStep
    NativeImageEnableModule registerNetModuleForNative() {
        //Compiling H2 to native requires activating the jdk.net module of the JDK
        return new NativeImageEnableModule("jdk.net");
    }

    @BuildStep
    void excludeNativeImageDirectives(BuildProducer<RemovedResourceBuildItem> removedResources) {
        removedResources.produce(new RemovedResourceBuildItem(ArtifactKey.fromString("org.hsqldb:hsqldb"),
                Set.of("META-INF/native-image/reflect-config.json")));
    }
    */

}