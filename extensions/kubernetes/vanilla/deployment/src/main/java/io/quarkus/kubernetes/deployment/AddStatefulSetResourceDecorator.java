
package io.quarkus.kubernetes.deployment;

import java.util.HashMap;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;

public class AddStatefulSetResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String name;
    private final PlatformConfiguration config;

    @Override
    public void visit(KubernetesListFluent<?> list) {
        list.addToItems(new StatefulSetBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withServiceName(name)
                .withNewSelector()
                .withMatchLabels(new HashMap<String, String>())
                .endSelector()
                .withNewTemplate()
                .withNewSpec()
                .withTerminationGracePeriodSeconds(10L)
                .addNewContainer()
                .withName(name)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build());
    }

    public AddStatefulSetResourceDecorator(String name, PlatformConfiguration config) {
        this.name = name;
        this.config = config;
    }
}
