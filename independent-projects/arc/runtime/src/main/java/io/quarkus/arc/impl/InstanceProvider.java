package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;

/**
 *
 * @author Martin Kouba
 */
public class InstanceProvider<T> implements InjectableReferenceProvider<Instance<T>> {

    private final Type requiredType;
    private final Set<Annotation> qualifiers;
    private final InjectableBean<?> targetBean;
    private final Set<Annotation> annotations;
    private final Supplier<Member> javaMemberSupplier;
    private final int position;
    private final boolean isTransient;

    public InstanceProvider(Type type, Set<Annotation> qualifiers, InjectableBean<?> targetBean, Set<Annotation> annotations,
            Supplier<Member> javaMemberSupplier, int position, boolean isTransient) {
        this.requiredType = type;
        this.qualifiers = qualifiers;
        this.targetBean = targetBean;
        this.annotations = annotations;
        this.javaMemberSupplier = javaMemberSupplier;
        this.position = position;
        this.isTransient = isTransient;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Instance<T> get(CreationalContext<Instance<T>> creationalContext) {
        InstanceImpl<T> instance = InstanceImpl.forInjection(targetBean, requiredType, qualifiers,
                CreationalContextImpl.unwrap(creationalContext),
                annotations, javaMemberSupplier.get(), position, isTransient);
        CreationalContextImpl.addDependencyToParent(InstanceBean.INSTANCE, instance,
                (CreationalContext) creationalContext);
        return instance;
    }

}
