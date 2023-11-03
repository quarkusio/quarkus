package io.quarkus.extest.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;

public class MapBuildTimeConfigBuildStep {

    public static final String TEST_MAP_CONFIG_MARKER = "test-map-config";
    public static final String INVOKED = "the test was invoked";

    @BuildStep
    void validate(BuildProducer<ConfigPropertyBuildItem> configProperties, TestBuildAndRunTimeConfig mapConfig) {
        Optional<String> pathToMarkerFile = ConfigProvider.getConfig().getOptionalValue("test-map-config", String.class);
        if (pathToMarkerFile.isPresent()) {
            assert mapConfig.mapMap.get("main-profile") != null;
            assert mapConfig.mapMap.get("main-profile").get("property") != null;
            assert mapConfig.mapMap.get("test-profile") != null;
            assert mapConfig.mapMap.get("test-profile").get("property") != null;

            try {
                Files.write(Paths.get(pathToMarkerFile.get()), INVOKED.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Unable to write to the marker file.");
            }
        }
    }
}
