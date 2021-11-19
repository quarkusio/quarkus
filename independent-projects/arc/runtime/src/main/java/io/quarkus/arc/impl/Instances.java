package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.CurrentInjectionPointProvider.InjectionPointImpl;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;

public final class Instances {

    static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[] {};

    static final Comparator<InjectableBean<?>> PRIORITY_COMPARATOR = Collections
            .reverseOrder(Comparator.comparingInt(InjectableBean::getPriority));

    private Instances() {
    }

    public static List<InjectableBean<?>> resolveBeans(Type requiredType, Set<Annotation> requiredQualifiers) {
        return resolveBeans(requiredType, requiredQualifiers.toArray(EMPTY_ANNOTATION_ARRAY));
    }

    public static List<InjectableBean<?>> resolveBeans(Type requiredType, Annotation... requiredQualifiers) {
        return ArcContainerImpl.instance()
                .getResolvedBeans(requiredType, requiredQualifiers)
                .stream()
                .filter(Predicate.not(InjectableBean::isSuppressed))
                .sorted(PRIORITY_COMPARATOR)
                .collect(Collectors.toUnmodifiableList());
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listOf(InjectableBean<?> targetBean, Type injectionPointType, Type requiredType,
            Set<Annotation> requiredQualifiers,
            CreationalContextImpl<?> creationalContext, Set<Annotation> annotations, Member javaMember, int position) {
        List<InjectableBean<?>> beans = resolveBeans(requiredType, requiredQualifiers);
        if (beans.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(beans.size());
        InjectionPoint prev = InjectionPointProvider
                .set(new InjectionPointImpl(injectionPointType, requiredType, requiredQualifiers, targetBean,
                        annotations, javaMember, position));
        try {
            for (InjectableBean<?> bean : beans) {
                list.add(getBeanInstance((CreationalContextImpl<T>) creationalContext, (InjectableBean<T>) bean));
            }
        } finally {
            InjectionPointProvider.set(prev);
        }

        return List.copyOf(list);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<InstanceHandle<T>> listOfHandles(InjectableBean<?> targetBean, Type injectionPointType,
            Type requiredType,
            Set<Annotation> requiredQualifiers,
            CreationalContextImpl<?> creationalContext, Set<Annotation> annotations, Member javaMember, int position) {
        List<InjectableBean<?>> beans = resolveBeans(requiredType, requiredQualifiers);
        if (beans.isEmpty()) {
            return Collections.emptyList();
        }
        Supplier<InjectionPoint> supplier = new Supplier<InjectionPoint>() {
            @Override
            public InjectionPoint get() {
                return new InjectionPointImpl(injectionPointType, requiredType, requiredQualifiers, targetBean,
                        annotations, javaMember, position);
            }
        };
        List<InstanceHandle<T>> list = new ArrayList<>(beans.size());
        for (InjectableBean<?> bean : beans) {
            list.add(getHandle((CreationalContextImpl<T>) creationalContext, (InjectableBean<T>) bean, supplier));
        }
        return List.copyOf(list);
    }

    private static <T> T getBeanInstance(CreationalContextImpl<T> parent, InjectableBean<T> bean) {
        CreationalContextImpl<T> ctx = parent.child(bean);
        T instance = bean.get(ctx);
        if (Dependent.class.equals(bean.getScope())) {
            CreationalContextImpl.addDependencyToParent(bean, instance, ctx);
        }
        return instance;
    }

    private static <T> InstanceHandle<T> getHandle(CreationalContextImpl<T> parent, InjectableBean<T> bean,
            Supplier<InjectionPoint> injectionPointSupplier) {
        CreationalContextImpl<T> ctx = parent.child(bean);
        return new LazyInstanceHandle<>(bean, ctx, parent, new Supplier<T>() {

            @Override
            public T get() {
                InjectionPoint prev = InjectionPointProvider
                        .set(injectionPointSupplier.get());
                try {
                    return bean.get(ctx);
                } finally {
                    InjectionPointProvider.set(prev);
                }
            }
        }, null);
    }

}
