
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;

public class AddDeploymentResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String name;
    private final PlatformConfiguration config;

    public AddDeploymentResourceDecorator(String name, PlatformConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public void visit(KubernetesListFluent<?> list) {
        DeploymentBuilder builder = list.buildItems().stream()
                .filter(this::containsDeploymentResource)
                .map(replaceExistingDeploymentResource(list))
                .findAny()
                .orElseGet(this::createDeploymentResource)
                .accept(DeploymentBuilder.class, this::initDeploymentResourceWithDefaults);

        list.addToItems(builder.build());
    }

    private boolean containsDeploymentResource(HasMetadata metadata) {
        return DEPLOYMENT.equalsIgnoreCase(metadata.getKind()) && name.equals(metadata.getMetadata().getName());
    }

    private void initDeploymentResourceWithDefaults(DeploymentBuilder builder) {
        DeploymentFluent<?>.SpecNested<DeploymentBuilder> spec = builder.editOrNewSpec();

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
            spec.editTemplate().editSpec().addNewContainer().withName(name).endContainer().endSpec().endTemplate();
        }

        spec.endSpec();
    }

    private DeploymentBuilder createDeploymentResource() {
        return new DeploymentBuilder().withNewMetadata().withName(name).endMetadata();
    }

    private Function<HasMetadata, DeploymentBuilder> replaceExistingDeploymentResource(KubernetesListFluent<?> list) {
        return metadata -> {
            list.removeFromItems(metadata);
            return new DeploymentBuilder((Deployment) metadata);
        };
    }

    private boolean containsContainerWithName(DeploymentFluent<?>.SpecNested<DeploymentBuilder> spec) {
        List<Container> containers = spec.buildTemplate().getSpec().getContainers();
        return containers != null && containers.stream().anyMatch(c -> name.equals(c.getName()));
    }
}
