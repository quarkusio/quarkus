package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;

/**
 *
 * @author Martin Kouba
 */
public class InstanceProvider<T> implements InjectableReferenceProvider<Instance<T>> {

    private final Type requiredType;

    private final Set<Annotation> qualifiers;

    public InstanceProvider(Type type, Set<Annotation> qualifiers) {
        this.requiredType = type;
        this.qualifiers = qualifiers;
    }

    @Override
    public Instance<T> get(CreationalContext<Instance<T>> creationalContext) {
        return new InstanceImpl<T>(requiredType, qualifiers, CreationalContextImpl.unwrap(creationalContext));
    }

}
