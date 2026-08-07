package io.quarkus.tests.simpleextension.deployment;

import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_BASE_URL;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_DEVSERVICES_PORT;
import static io.quarkus.tests.simpleextension.Constants.QUARKUS_SIMPLE_EXTENSION_STATIC_THING;
import static io.quarkus.tests.simpleextension.Constants.SIMPLE_EXTENSION_CLASSLOADER_ON_SERVICE_START;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.StartableContainer;

public class SimpleDevServicesProcessor {

    private static final String FEATURE = "Simples";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createContainer() {
        Optional<Integer> fixedPort = ConfigProvider.getConfig()
                .getOptionalValue(QUARKUS_SIMPLE_EXTENSION_DEVSERVICES_PORT, Integer.class);

        return DevServicesResultBuildItem.owned()
                .feature("quarkus-Basic")
                .serviceName(FEATURE)
                .startable(() -> {
                    SimpleContainer container = new SimpleContainer(fixedPort);
                    return new StartableContainer<>(container, c -> c.getConnectionInfo());
                })
                .config(Map.of(QUARKUS_SIMPLE_EXTENSION_STATIC_THING, "some value"))
                .configProvider(Map.of(QUARKUS_SIMPLE_EXTENSION_BASE_URL,
                        c -> c.getConnectionInfo(), SIMPLE_EXTENSION_CLASSLOADER_ON_SERVICE_START,
                        c -> ((SimpleContainer) c.getContainer()).getClassLoaderNameOnStart()))
                .build();

    }
}
