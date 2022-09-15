package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ResourceReferenceProvider;
import jakarta.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Represents a placeholder for all suppored non-CDI injection points.
 *
 * @author Martin Kouba
 * @see ResourceReferenceProvider
 */
public class ResourceProvider implements InjectableReferenceProvider<Object> {

    private final Type type;

    private final Set<Annotation> annotations;

    public ResourceProvider(Type type, Set<Annotation> annotations) {
        this.type = type;
        this.annotations = annotations;
    }

    @Override
    public Object get(CreationalContext<Object> creationalContext) {
        InstanceHandle<Object> instance = ArcContainerImpl.instance().getResource(type, annotations);
        if (instance != null) {
            CreationalContextImpl<?> ctx = CreationalContextImpl.unwrap(creationalContext);
            if (ctx.getParent() != null) {
                ctx.getParent().addDependentInstance(instance);
            }
            return instance.get();
        }
        // TODO log a warning that a resource cannot be injected
        return null;
    }

}
