package io.quarkus.openshift.deployment;

import java.util.Collections;

import io.quarkus.container.image.deployment.devconsole.RebuildHandler;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;

public class OpenshiftDevConsoleProcessor {

    @BuildStep
    DevConsoleRouteBuildItem builder() {
        return new DevConsoleRouteBuildItem("deploy", "POST",
                new RebuildHandler(Collections.singletonMap("quarkus.kubernetes.deploy", "true")));
    }
}
