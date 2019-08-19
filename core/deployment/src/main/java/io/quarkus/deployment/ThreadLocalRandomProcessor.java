package io.quarkus.deployment;

import java.util.concurrent.ThreadLocalRandom;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;

public class ThreadLocalRandomProcessor {
    @BuildStep
    RuntimeReinitializedClassBuildItem registerThreadLocalRandomReinitialize() {
        // ThreadLocalRandom is bugged currently and doesn't reset the seeder
        // See https://github.com/oracle/graal/issues/1614 for more details
        return new RuntimeReinitializedClassBuildItem(ThreadLocalRandom.class.getName());
    }
}
