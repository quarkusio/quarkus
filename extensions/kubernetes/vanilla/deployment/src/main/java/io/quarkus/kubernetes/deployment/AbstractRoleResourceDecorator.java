package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.RBAC_API_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.ROLE;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;

abstract class AbstractRoleResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    protected final String deploymentName;
    protected final String name;
    protected final Map<String, String> labels;

    public AbstractRoleResourceDecorator(String deploymentName, String name, Map<String, String> labels) {
        this.labels = labels;
        this.deploymentName = deploymentName;
        this.name = name;
    }

    public Optional<Map<String, String>> preVisit(KubernetesListBuilder list) {
        if (contains(list, RBAC_API_VERSION, ROLE, name)) {
            return Optional.empty();
        }

        Map<String, String> roleLabels = new HashMap<>();
        roleLabels.putAll(labels);
        getDeploymentMetadata(list, deploymentName)
                .map(ObjectMeta::getLabels)
                .ifPresent(roleLabels::putAll);

        return Optional.of(roleLabels);
    }
}
