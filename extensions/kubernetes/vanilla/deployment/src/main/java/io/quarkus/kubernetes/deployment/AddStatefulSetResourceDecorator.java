
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.STATEFULSET;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent;

public class AddStatefulSetResourceDecorator extends ResourceProvidingDecorator<KubernetesListFluent<?>> {

    private final String name;
    private final PlatformConfiguration config;

    public AddStatefulSetResourceDecorator(String name, PlatformConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public void visit(KubernetesListFluent<?> list) {
        StatefulSetBuilder builder = list.buildItems().stream()
                .filter(this::containsStatefulSetResource)
                .map(replaceExistingStatefulSetResource(list))
                .findAny()
                .orElseGet(this::createStatefulSetResource)
                .accept(StatefulSetBuilder.class, this::initStatefulSetResourceWithDefaults);

        list.addToItems(builder.build());
    }

    private boolean containsStatefulSetResource(HasMetadata metadata) {
        return STATEFULSET.equalsIgnoreCase(metadata.getKind()) && name.equals(metadata.getMetadata().getName());
    }

    private void initStatefulSetResourceWithDefaults(StatefulSetBuilder builder) {
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
        if (Strings.isNullOrEmpty(spec.getServiceName())) {
            spec.withServiceName(name);
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

    private StatefulSetBuilder createStatefulSetResource() {
        return new StatefulSetBuilder().withNewMetadata().withName(name).endMetadata();
    }

    private Function<HasMetadata, StatefulSetBuilder> replaceExistingStatefulSetResource(KubernetesListFluent<?> list) {
        return metadata -> {
            list.removeFromItems(metadata);
            return new StatefulSetBuilder((StatefulSet) metadata);
        };
    }

    private boolean containsContainerWithName(StatefulSetFluent<?>.SpecNested<StatefulSetBuilder> spec) {
        List<Container> containers = spec.buildTemplate().getSpec().getContainers();
        return containers == null || containers.stream().anyMatch(c -> name.equals(c.getName()));
    }
}
