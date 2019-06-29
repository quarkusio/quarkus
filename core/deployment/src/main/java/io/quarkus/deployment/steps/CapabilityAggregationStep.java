package io.quarkus.deployment.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;

public class CapabilityAggregationStep {

    @Inject
    BuildProducer<Capabilities> producer;

    @BuildStep
    public void build(List<CapabilityBuildItem> capabilities) throws Exception {
        Set<String> present = new HashSet<>();
        for (CapabilityBuildItem capability : capabilities) {
            present.add(capability.getName());
        }

        producer.produce(new Capabilities(present));

    }
}
