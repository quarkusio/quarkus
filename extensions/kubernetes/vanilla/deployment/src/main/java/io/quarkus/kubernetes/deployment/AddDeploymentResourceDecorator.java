
package io.quarkus.kubernetes.deployment;

import java.util.HashMap;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public class AddDeploymentResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String name;
    private final PlatformConfiguration config;

    @Override
    public void visit(KubernetesListFluent<?> list) {
        list.addToItems(new DeploymentBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .withMatchLabels(new HashMap<String, String>())
                .endSelector()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName(name)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build());
    }

    public AddDeploymentResourceDecorator(String name, PlatformConfiguration config) {
        this.name = name;
        this.config = config;
    }
}
