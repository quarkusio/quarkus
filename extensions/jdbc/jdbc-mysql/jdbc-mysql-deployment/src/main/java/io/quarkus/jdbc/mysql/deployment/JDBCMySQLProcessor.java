package io.quarkus.jdbc.mysql.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;

public class JDBCMySQLProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JDBC_MYSQL);
    }

    @BuildStep
    SubstrateResourceBuildItem resource() {
        return new SubstrateResourceBuildItem("com/mysql/cj/util/TimeZoneMapping.properties");
    }

    @BuildStep
    NativeEnableAllCharsetsBuildItem enableAllCharsets() {
        return new NativeEnableAllCharsetsBuildItem();
    }
}
