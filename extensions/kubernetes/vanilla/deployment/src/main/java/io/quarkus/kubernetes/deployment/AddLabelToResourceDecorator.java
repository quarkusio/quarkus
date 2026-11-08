package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;

/**
 * Adds a label to a named resource.
 * <p>
 * Unlike {@link AddLabelDecorator}, equality includes the resource name. Dekorate stores decorators in a
 * {@link java.util.TreeSet}, and {@link AddLabelDecorator#equals(Object)} only compares the label key/value, which
 * would drop additional decorators that apply the same label to init-task resources.
 */
class AddLabelToResourceDecorator extends NamedResourceDecorator<ObjectMetaFluent<?>> {

    private final String resourceName;
    private final String key;
    private final String value;

    AddLabelToResourceDecorator(String resourceName, String key, String value) {
        super(resourceName);
        this.resourceName = resourceName;
        this.key = key;
        this.value = value;
    }

    @Override
    public void andThenVisit(ObjectMetaFluent<?> meta, ObjectMeta objectMeta) {
        meta.addToLabels(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AddLabelToResourceDecorator other)) {
            return false;
        }
        return Objects.equals(resourceName, other.resourceName)
                && Objects.equals(key, other.key)
                && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, key, value);
    }
}
