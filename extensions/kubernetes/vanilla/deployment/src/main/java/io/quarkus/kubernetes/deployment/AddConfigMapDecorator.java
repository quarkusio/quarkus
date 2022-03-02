package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

/**
 * This class was created to workaround https://github.com/dekorateio/dekorate/issues/869.
 * Once this issue is fixed, we can delete this and use the provided by Dekorate.
 */
public class AddConfigMapDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private static final String DEFAULT_NAMESPACE = null;

    private final String name;
    private final String namespace;

    public AddConfigMapDecorator(String name) {
        this(name, DEFAULT_NAMESPACE);
    }

    public AddConfigMapDecorator(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public void visit(KubernetesListBuilder list) {
        if (contains(list, "v1", "ConfigMap", name)) {
            return;
        }

        list.addNewConfigMapItem()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .endConfigMapItem();
    }
}
