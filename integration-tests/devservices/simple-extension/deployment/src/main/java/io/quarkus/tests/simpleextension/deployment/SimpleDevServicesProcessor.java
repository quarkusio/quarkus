package io.quarkus.tests.simpleextension.deployment;

import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_BASE_URL;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_STATIC_THING;

import java.util.Map;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;

public class SimpleDevServicesProcessor {

    private static final String FEATURE = "Simples";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createContainer() {

        return DevServicesResultBuildItem.owned()
                .feature("quarkus-Basic")
                .serviceName(FEATURE)
                .startable(SimpleContainer::new) // Builds could be speeded up a bit by using an in-process service, but coverage is probably better with a container
                .config(Map.of(QUARKUS_SIMPLE_EXTENSION_STATIC_THING, "some value"))
                .configProvider(Map.of(QUARKUS_SIMPLE_EXTENSION_BASE_URL,
                        c -> c.getConnectionInfo()))
                .build();

    }
}
