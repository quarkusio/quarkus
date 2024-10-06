package io.quarkus.kubernetes.deployment;

import java.util.*;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

public class AddNamespaceDecorator extends Decorator<KubernetesListBuilder> {

    private static final Set<String> CLUSTERED_RESOURCES = Set.of("ClusterRoleBinding", "ClusterRole");
    private final String namespace;

    public AddNamespaceDecorator(String namespace) {
        this.namespace = Objects.requireNonNull(namespace);
    }

    @Override
    public void visit(KubernetesListBuilder list) {
        List<HasMetadata> buildItems = list.buildItems()
                .stream()
                .filter(obj -> obj instanceof HasMetadata)
                .peek(obj -> {
                    if (isEligibleForChangingNamespace(obj)) {
                        obj.setMetadata(obj.getMetadata().edit().withNamespace(namespace).build());
                    }
                }).collect(Collectors.toList());
        list.withItems(buildItems);
    }

    private boolean isEligibleForChangingNamespace(HasMetadata obj) {
        return obj.getMetadata().getNamespace() == null && !CLUSTERED_RESOURCES.contains(obj.getKind());
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
