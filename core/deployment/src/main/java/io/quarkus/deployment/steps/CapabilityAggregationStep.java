package io.quarkus.deployment.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;

public class CapabilityAggregationStep {

    @BuildStep
    Capabilities build(List<CapabilityBuildItem> capabilities) throws Exception {
        Set<String> present = new HashSet<>();
        for (CapabilityBuildItem capability : capabilities) {
            present.add(capability.getName());
        }
        return new Capabilities(present);
    }
}
