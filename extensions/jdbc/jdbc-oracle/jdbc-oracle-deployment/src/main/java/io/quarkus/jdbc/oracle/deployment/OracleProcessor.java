package io.quarkus.jdbc.oracle.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;

class OracleProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.JDBC_ORACLE);
    }

    @BuildStep
    SubstrateResourceBundleBuildItem includeResourceBundle() {
        return new SubstrateResourceBundleBuildItem("oracle.net.jdbc.nl.mesg.NLSR");
    }

    @BuildStep
    void initializeAtRuntime(BuildProducer<RuntimeInitializedClassBuildItem> initializeAtRuntime) {
        initializeAtRuntime.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.BlockSource"));
        initializeAtRuntime.produce(new RuntimeInitializedClassBuildItem(
                "oracle.jdbc.driver.BlockSource$ThreadedCachingBlockSource$BlockReleaser"));
        initializeAtRuntime.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.NamedTypeAccessor$XMLFactory"));
        initializeAtRuntime.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.OracleTimeoutThreadPerVM"));
        initializeAtRuntime.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.SQLUtil$XMLFactory"));
        initializeAtRuntime.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.TimeoutInterruptHandler"));
    }

}
