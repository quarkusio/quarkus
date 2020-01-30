package io.quarkus.jdbc.mssql.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class MsSQLProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JDBC_MSSQL);
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
