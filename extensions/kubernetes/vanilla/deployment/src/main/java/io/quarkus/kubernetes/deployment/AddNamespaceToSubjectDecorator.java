package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.decorator.AddRoleBindingResourceDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.SubjectFluent;

public class AddNamespaceToSubjectDecorator extends NamedResourceDecorator<SubjectFluent<?>> {

    private final String namespace;

    public AddNamespaceToSubjectDecorator(String namespace) {
        super(ANY);
        this.namespace = Objects.requireNonNull(namespace);
    }

    public AddNamespaceToSubjectDecorator(String name, String namespace) {
        super(name);
        this.namespace = Objects.requireNonNull(namespace);
    }

    @Override
    public void andThenVisit(SubjectFluent<?> subject, ObjectMeta resourceMeta) {
        subject.withNamespace(namespace);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddRoleBindingResourceDecorator.class };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AddNamespaceToSubjectDecorator other = (AddNamespaceToSubjectDecorator) obj;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        return true;
    }
}
