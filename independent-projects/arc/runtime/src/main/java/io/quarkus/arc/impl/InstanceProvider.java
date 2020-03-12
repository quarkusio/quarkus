package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
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
    private final InjectableBean<?> targetBean;
    private final Set<Annotation> annotations;
    private final Member javaMember;
    private final int position;

    public InstanceProvider(Type type, Set<Annotation> qualifiers, InjectableBean<?> targetBean, Set<Annotation> annotations,
            Member javaMember, int position) {
        this.requiredType = type;
        this.qualifiers = qualifiers;
        this.targetBean = targetBean;
        this.annotations = annotations;
        this.javaMember = javaMember;
        this.position = position;
    }

    @Override
    public Instance<T> get(CreationalContext<Instance<T>> creationalContext) {
        return new InstanceImpl<T>(targetBean, requiredType, qualifiers, CreationalContextImpl.unwrap(creationalContext),
                annotations, javaMember, position);
    }

}
