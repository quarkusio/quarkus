package io.quarkus.tests.dependentextension.deployment;

import static io.quarkus.tests.dependentextension.Constants.QUARKUS_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_DEPENDENT_EXTENSION_SEES_DEPENDENCY;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_UNSATISFIED_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL;
import static io.quarkus.tests.dependentextension.Constants.QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY;

import java.util.Map;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;

public class DependentDevServicesProcessor {

    private static final String FEATURE = "Needy";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createDependentContainer() {

        return DevServicesResultBuildItem.owned()
                .feature(FEATURE)
                .serviceName(FEATURE)
                .startable(() -> new DependentContainer()) // Builds could be speeded up a bit by using an in-process service, but coverage is probably better with a container
                .configProvider(Map.of(QUARKUS_DEPENDENT_EXTENSION_BASE_URL,
                        c -> c.getConnectionInfo(), QUARKUS_DEPENDENT_EXTENSION_SEES_DEPENDENCY,
                        c -> String.valueOf(c.isDependencyAvailable())))
                .dependsOnConfig("acme.simpleextension.base-url", (s, v) -> s.setOtherUrl(v))
                .build();

    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createDependentContainerWithUnsatisfiedDependencies() {

        return DevServicesResultBuildItem.owned()
                .feature(FEATURE)
                .serviceName(FEATURE + "unsat")
                .startable(() -> new DependentContainer()) // Builds could be speeded up a bit by using an in-process service, but coverage is probably better with a container
                .configProvider(Map.of(QUARKUS_UNSATISFIED_DEPENDENT_EXTENSION_BASE_URL,
                        c -> c.getConnectionInfo()))
                .dependsOnConfig("impossible", (s, v) -> s.setOtherUrl(v))
                .build();

    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createDependentContainerWithOptionalDependencies() {

        return DevServicesResultBuildItem.owned()
                .feature("quarkus-dependent")
                .serviceName(FEATURE + "opt")
                .startable(() -> new DependentContainer()) // Builds could be speeded up a bit by using an in-process service, but coverage is probably better with a container
                .configProvider(Map.of(QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL,
                        c -> c.getConnectionInfo(), QUARKUS_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY,
                        c -> String.valueOf(c.isDependencyAvailable())))
                .dependsOnConfig("acme.simpleextension.base-url", (s, v) -> s.setOtherUrl(v), true)
                .build();

    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem createDependentContainerWithUnsatisfiedOptionalDependencies() {

        return DevServicesResultBuildItem.owned()
                .feature(FEATURE)
                .serviceName(FEATURE + "unsat-opt")
                .startable(() -> new OptionallyDependentContainer()) // Builds could be speeded up a bit by using an in-process service, but coverage is probably better with a container
                .configProvider(Map.of(QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_BASE_URL,
                        c -> c.getConnectionInfo(), QUARKUS_UNSATISFIED_OPTIONAL_DEPENDENT_EXTENSION_SEES_DEPENDENCY,
                        c -> String.valueOf(c.isDependencyAvailable())))
                .dependsOnConfig("impossible", (s, v) -> s.setOtherUrl(v), true)
                .build();

    }
}
