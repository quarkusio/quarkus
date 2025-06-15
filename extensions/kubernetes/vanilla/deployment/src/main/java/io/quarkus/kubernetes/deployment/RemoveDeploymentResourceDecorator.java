
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;

import java.util.List;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class RemoveDeploymentResourceDecorator extends Decorator<KubernetesListBuilder> {
    private String name;

    public RemoveDeploymentResourceDecorator(String name) {
        this.name = name;
    }

    @Override
    public void visit(KubernetesListBuilder builder) {
        List<HasMetadata> deployments = builder.buildItems().stream().filter(
                d -> d != null && d.getKind().equals(DEPLOYMENT) && d.getMetadata().getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());

        builder.removeAllFromItems(deployments);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }
}
