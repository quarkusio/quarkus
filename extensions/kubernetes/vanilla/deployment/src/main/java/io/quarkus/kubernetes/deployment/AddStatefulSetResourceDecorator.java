
package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.quarkus.runtime.util.StringUtil;

public class AddStatefulSetResourceDecorator
        extends BaseAddDeploymentResourceDecorator<StatefulSet, StatefulSetBuilder, PlatformConfiguration> {
    public AddStatefulSetResourceDecorator(String name, PlatformConfiguration config, DeploymentResourceKind toRemove) {
        super(name, DeploymentResourceKind.StatefulSet, config, toRemove);
    }

    @Override
    protected StatefulSetBuilder builderWithName(String name) {
        return new StatefulSetBuilder().withNewMetadata().withName(name).endMetadata();
    }

    @Override
    protected void initBuilderWithDefaults(StatefulSetBuilder builder) {
        final var spec = builder.editOrNewSpec();

        // service name
        if (StringUtil.isNullOrEmpty(spec.getServiceName())) {
            spec.withServiceName(name());
        }

        // match labels for selector
        initSelectorMatchLabels(spec.editOrNewSelector())
                .endSelector();

        // replicas
        spec.withReplicas(replicas(spec.getReplicas(), replicasAwareOrNull()));

        // configure main application pod and container
        configurePodSpec(spec.editOrNewTemplate().editOrNewSpec())
                .endSpec().endTemplate();

        spec.endSpec();
    }
}
