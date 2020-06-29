package io.quarkus.jdbc.postgresql.deployment;

import io.quarkus.agroal.deployment.JdbcDriverBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;

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
    NativeImageConfigBuildItem build() {
        return NativeImageConfigBuildItem.builder()
                .addNativeImageSystemProperty("QuarkusWithJcc", "myquarkusJCCcode")
                //                .addRuntimeInitializedClass("com.ibm.db2.jcc.am.Configuration")
                //                .addRuntimeInitializedClass("com.ibm.db2.jcc.am.GlobalProperties")
                //                .addRuntimeInitializedClass("com.ibm.db2.jcc.am.TimerServices")
                .build();
    }
}
