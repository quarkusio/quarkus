
package io.quarkus.kubernetes.deployment;

import java.util.HashMap;
import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent;
import io.quarkus.runtime.util.StringUtil;

public class AddStatefulSetResourceDecorator extends BaseAddDeploymentResourceDecorator<StatefulSet, StatefulSetBuilder, Void> {
    public AddStatefulSetResourceDecorator(String name, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.StatefulSet, null, toRemove);
    }

    @Override
    protected StatefulSetBuilder builderWithName(String name) {
        return new StatefulSetBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(StatefulSetBuilder builder, Void config) {
        StatefulSetFluent<?>.SpecNested<StatefulSetBuilder> spec = builder.editOrNewSpec();

        spec.editOrNewSelector()
                .endSelector()
                .editOrNewTemplate()
                .editOrNewSpec()
                .endSpec()
                .editOrNewMetadata()
                .endMetadata()
                .endTemplate();

        // defaults for:
        // - replicas
        if (spec.getReplicas() == null) {
            spec.withReplicas(1);
        }
        // - service name
        if (StringUtil.isNullOrEmpty(spec.getServiceName())) {
            spec.withServiceName(name());
        }
        // - match labels
        if (spec.buildSelector().getMatchLabels() == null) {
            spec.editSelector().withMatchLabels(new HashMap<>()).endSelector();
        }
        // - termination grace period seconds
        if (spec.buildTemplate().getSpec().getTerminationGracePeriodSeconds() == null) {
            spec.editTemplate().editSpec().withTerminationGracePeriodSeconds(10L).endSpec().endTemplate();
        }
        // - container
        if (!containsContainerWithName(spec)) {
            spec.editTemplate().editSpec().addNewContainer().withName(name()).endContainer().endSpec().endTemplate();
        }

        spec.endSpec();
    }

    private boolean containsContainerWithName(StatefulSetFluent<?>.SpecNested<StatefulSetBuilder> spec) {
        List<Container> containers = spec.buildTemplate().getSpec().getContainers();
        return containers == null || containers.stream().anyMatch(c -> name().equals(c.getName()));
    }
}
