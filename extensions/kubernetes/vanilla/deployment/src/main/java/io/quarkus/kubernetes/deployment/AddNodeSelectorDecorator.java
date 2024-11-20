package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpecFluent;

public class AddNodeSelectorDecorator extends NamedResourceDecorator<PodSpecFluent<?>> {
    private final String nodeSelectorKey;
    private final String nodeSelectorValue;

    public AddNodeSelectorDecorator(String deploymentName, String nodeSelectorKey, String nodeSelectorValue) {
        super(deploymentName);
        this.nodeSelectorKey = nodeSelectorKey;
        this.nodeSelectorValue = nodeSelectorValue;
    }

    public void andThenVisit(PodSpecFluent<?> podSpec, ObjectMeta resourceMeta) {
        if (Strings.isNotNullOrEmpty(nodeSelectorKey) && Strings.isNotNullOrEmpty(nodeSelectorValue)) {
            podSpec.removeFromNodeSelector(nodeSelectorKey);
            podSpec.addToNodeSelector(nodeSelectorKey, nodeSelectorValue);
        }
    }
}
