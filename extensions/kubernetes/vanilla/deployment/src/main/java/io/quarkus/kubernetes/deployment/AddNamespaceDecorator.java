package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class AddNamespaceDecorator extends Decorator<KubernetesListBuilder> {

    private final String namespace;

    public AddNamespaceDecorator(String namespace) {
        this.namespace = Objects.requireNonNull(namespace);
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        List<HasMetadata> buildItems = list.buildItems()
                .stream()
                .peek(obj -> {
                    if (obj instanceof Namespaced) {
                        final ObjectMeta metadata = obj.getMetadata();
                        if (metadata.getNamespace() == null) {
                            metadata.setNamespace(namespace);
                            obj.setMetadata(metadata);
                        }
                    }
                }).collect(Collectors.toList());
        list.withItems(buildItems);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AddNamespaceDecorator that = (AddNamespaceDecorator) o;
        return namespace.equals(that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace);
    }

}
