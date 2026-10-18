package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.RemoveLabelDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;

/**
 * Removes a label from a named resource.
 * <p>
 * Unlike {@link RemoveLabelDecorator}, equality includes the resource name. Dekorate stores decorators in a
 * {@link java.util.TreeSet}, and {@link RemoveLabelDecorator#equals(Object)} only compares the label key, which would
 * drop additional removals for init-task resources (e.g. stripping {@code app.kubernetes.io/version} in idempotent
 * mode).
 */
class RemoveLabelFromResourceDecorator extends NamedResourceDecorator<ObjectMetaFluent<?>> {

    private final String resourceName;
    private final String key;

    RemoveLabelFromResourceDecorator(String resourceName, String key) {
        super(resourceName);
        this.resourceName = resourceName;
        this.key = key;
    }

    @Override
    public void andThenVisit(ObjectMetaFluent<?> meta, ObjectMeta objectMeta) {
        meta.removeFromLabels(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddLabelDecorator.class, AddLabelToResourceDecorator.class };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RemoveLabelFromResourceDecorator other)) {
            return false;
        }
        return Objects.equals(resourceName, other.resourceName) && Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, key);
    }
}
