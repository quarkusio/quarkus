package org.jboss.shamrock.deployment.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.CapabilityBuildItem;

public class CapabilityAggregationStep {

    @Inject
    BuildProducer<Capabilities> producer;

    @BuildStep
    public void build(List<CapabilityBuildItem> capabilitites) throws Exception {
        Set<String> present = new HashSet<>();
        for (CapabilityBuildItem i : capabilitites) {
            present.add(i.getName());
        }

        producer.produce(new Capabilities(present));

    }
}
