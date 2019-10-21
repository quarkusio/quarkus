package io.quarkus.jdbc.mysql.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

public class JDBCMySQLProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JDBC_MYSQL);
    }

    @BuildStep
    NativeImageResourceBuildItem resource() {
        return new NativeImageResourceBuildItem("com/mysql/cj/util/TimeZoneMapping.properties");
    }

    @BuildStep
    NativeEnableAllCharsetsBuildItem enableAllCharsets() {
        return new NativeEnableAllCharsetsBuildItem();
    }
}
