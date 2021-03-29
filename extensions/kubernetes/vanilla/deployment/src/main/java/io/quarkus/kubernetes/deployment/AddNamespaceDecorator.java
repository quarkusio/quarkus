package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class AddNamespaceDecorator extends NamedResourceDecorator<ObjectMetaBuilder> {

    private final String namespace;

    public AddNamespaceDecorator(String namespace) {
        this.namespace = Objects.requireNonNull(namespace);
    }

    @Override
    public void andThenVisit(ObjectMetaBuilder builder, ObjectMeta resourceMeta) {
        builder.withNamespace(namespace);
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
