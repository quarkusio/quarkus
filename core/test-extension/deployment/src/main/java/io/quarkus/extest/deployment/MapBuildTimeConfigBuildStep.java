package io.quarkus.extest.deployment;

import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;

public class MapBuildTimeConfigBuildStep {
    @BuildStep
    void validate(BuildProducer<ConfigPropertyBuildItem> configProperties, TestBuildAndRunTimeConfig mapConfig) {
        assert mapConfig.mapMap.get("main-profile") != null;
        assert mapConfig.mapMap.get("main-profile").get("property") != null;
        assert mapConfig.mapMap.get("test-profile") != null;
        assert mapConfig.mapMap.get("test-profile").get("property") != null;
    }
}
