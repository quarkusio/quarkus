package io.quarkus.jdbc.mssql.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;

public class MsSQLProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JDBC_MSSQL);
    }

    @BuildStep
    void nativeResources(BuildProducer<SubstrateResourceBundleBuildItem> resources,
            BuildProducer<NativeEnableAllCharsetsBuildItem> nativeEnableAllCharsets) {
        resources.produce(new SubstrateResourceBundleBuildItem("com.microsoft.sqlserver.jdbc.SQLServerResource"));
        nativeEnableAllCharsets.produce(new NativeEnableAllCharsetsBuildItem());
    }

}
