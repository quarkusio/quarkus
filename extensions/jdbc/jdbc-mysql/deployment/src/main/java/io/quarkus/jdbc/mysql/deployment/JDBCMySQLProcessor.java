package io.quarkus.jdbc.mysql.deployment;

import io.quarkus.agroal.deployment.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllTimeZonesBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.jdbc.mysql.runtime.MySQLAgroalConnectionConfigurer;

public class JDBCMySQLProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JDBC_MYSQL);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver,
            SslNativeConfigBuildItem sslNativeConfigBuildItem) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.MYSQL, "com.mysql.cj.jdbc.Driver",
                "com.mysql.cj.jdbc.MysqlXADataSource"));
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            Capabilities capabilities) {
        if (capabilities.isCapabilityPresent(Capabilities.AGROAL)) {
            additionalBeans.produce(new AdditionalBeanBuildItem.Builder().addBeanClass(MySQLAgroalConnectionConfigurer.class)
                    .setDefaultScope(BuiltinScope.APPLICATION.getName())
                    .build());
        }
    }

    @BuildStep
    NativeImageResourceBuildItem resource() {
        return new NativeImageResourceBuildItem("com/mysql/cj/util/TimeZoneMapping.properties");
    }

    @BuildStep
    NativeImageEnableAllCharsetsBuildItem enableAllCharsets() {
        return new NativeImageEnableAllCharsetsBuildItem();
    }

    @BuildStep
    NativeImageEnableAllTimeZonesBuildItem enableAllTimeZones() {
        return new NativeImageEnableAllTimeZonesBuildItem();
    }
}
