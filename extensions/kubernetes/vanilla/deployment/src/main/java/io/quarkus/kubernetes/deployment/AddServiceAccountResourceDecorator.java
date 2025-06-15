package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.SERVICE_ACCOUNT;

import java.util.HashMap;
import java.util.Map;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class AddServiceAccountResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private final String deploymentName;
    private final String name;
    private final String namespace;
    private final Map<String, String> labels;

    public AddServiceAccountResourceDecorator(String deploymentName, String name, String namespace,
            Map<String, String> labels) {
        this.deploymentName = deploymentName;
        this.name = name;
        this.namespace = namespace;
        this.labels = labels;
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        if (contains(list, "v1", SERVICE_ACCOUNT, name)) {
            return;
        }

        Map<String, String> saLabels = new HashMap<>();
        saLabels.putAll(labels);
        getDeploymentMetadata(list, deploymentName)
                .map(ObjectMeta::getLabels)
                .ifPresent(saLabels::putAll);

        list.addNewServiceAccountItem()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(saLabels)
                .endMetadata()
                .endServiceAccountItem();
    }
}
