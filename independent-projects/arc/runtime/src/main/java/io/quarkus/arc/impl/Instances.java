package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;

public final class Instances {

    static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[] {};

    static final Comparator<InjectableBean<?>> PRIORITY_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(InjectableBean<?> ib1, InjectableBean<?> ib2) {
            return Integer.compare(ib2.getPriority(), ib1.getPriority());
        }
    };

    private Instances() {
    }

    public static List<InjectableBean<?>> resolveBeans(Type requiredType, Set<Annotation> requiredQualifiers) {
        return resolveBeans(requiredType, requiredQualifiers.toArray(EMPTY_ANNOTATION_ARRAY));
    }

    public static List<InjectableBean<?>> resolveBeans(Type requiredType, Annotation... requiredQualifiers) {
        Set<InjectableBean<?>> resolvedBeans = ArcContainerImpl.instance()
                .getResolvedBeans(requiredType, requiredQualifiers);
        List<InjectableBean<?>> nonSuppressed = new ArrayList<>(resolvedBeans.size());
        for (InjectableBean<?> injectableBean : resolvedBeans) {
            if (!injectableBean.isSuppressed()) {
                nonSuppressed.add(injectableBean);
            }
        }
        nonSuppressed.sort(PRIORITY_COMPARATOR);
        return List.copyOf(nonSuppressed);
    }

    private static List<InjectableBean<?>> resolveAllBeans(Type requiredType, Set<Annotation> requiredQualifiers) {
        if (requiredQualifiers == null || requiredQualifiers.isEmpty()) {
            // If no qualifier is specified then @Any is used
            return resolveBeans(requiredType, new Annotation[] { Any.Literal.INSTANCE });
        }
        return resolveBeans(requiredType, requiredQualifiers.toArray(EMPTY_ANNOTATION_ARRAY));
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listOf(InjectableBean<?> targetBean, Type injectionPointType, Type requiredType,
            Set<Annotation> requiredQualifiers,
            CreationalContext<T> creationalContext, Set<Annotation> annotations, Member javaMember, int position,
            boolean isTransient) {
        List<InjectableBean<?>> beans = resolveAllBeans(requiredType, requiredQualifiers);
        if (beans.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(beans.size());
        InjectionPoint prev = InjectionPointProvider
                .set(new InjectionPointImpl(injectionPointType, requiredType, requiredQualifiers, targetBean,
                        annotations, javaMember, position, isTransient));
        try {
            for (InjectableBean<?> bean : beans) {
                list.add(getBeanInstance(CreationalContextImpl.unwrap(creationalContext), (InjectableBean<T>) bean));
            }
        } finally {
            InjectionPointProvider.set(prev);
        }

        return List.copyOf(list);
    }

    public static <T> List<InstanceHandle<T>> listOfHandles(InjectableBean<?> targetBean, Type injectionPointType,
            Type requiredType, Set<Annotation> requiredQualifiers,
            CreationalContext<T> creationalContext, Set<Annotation> annotations, Member javaMember, int position,
            boolean isTransient) {
        Supplier<InjectionPoint> supplier = new Supplier<InjectionPoint>() {
            @Override
            public InjectionPoint get() {
                return new InjectionPointImpl(injectionPointType, requiredType, requiredQualifiers, targetBean,
                        annotations, javaMember, position, isTransient);
            }
        };
        return listOfHandles(supplier, requiredType, requiredQualifiers, creationalContext);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<InstanceHandle<T>> listOfHandles(Supplier<InjectionPoint> injectionPoint, Type requiredType,
            Set<Annotation> requiredQualifiers,
            CreationalContext<T> creationalContext) {
        List<InjectableBean<?>> beans = resolveAllBeans(requiredType, requiredQualifiers);
        if (beans.isEmpty()) {
            return Collections.emptyList();
        }
        List<InstanceHandle<T>> list = new ArrayList<>(beans.size());
        for (InjectableBean<?> bean : beans) {
            list.add(getHandle(CreationalContextImpl.unwrap(creationalContext), (InjectableBean<T>) bean, injectionPoint));
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
