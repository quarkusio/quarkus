package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

/**
 * Adds an annotation to a named resource.
 * <p>
 * Unlike {@link AddAnnotationDecorator}, equality includes the resource name so the same annotation can be applied to
 * multiple init-task resources without being deduplicated by dekorate's {@link java.util.TreeSet}.
 */
class AddAnnotationToResourceDecorator extends NamedResourceDecorator<ObjectMetaBuilder> {

    private final String resourceName;
    private final String key;
    private final String value;

    AddAnnotationToResourceDecorator(String resourceName, String key, String value) {
        super(resourceName);
        this.resourceName = resourceName;
        this.key = key;
        this.value = value;
    }

    @Override
    public void andThenVisit(ObjectMetaBuilder meta, ObjectMeta objectMeta) {
        meta.addToAnnotations(key, value);
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
        if (!(obj instanceof AddAnnotationToResourceDecorator other)) {
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
