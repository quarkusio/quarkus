package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.smallrye.common.annotation.SuppressForbidden;

@SuppressForbidden(reason = """
        In `TYPE_COMPARATOR`, we first use `Type.name()`, which should distinguish most cases, but there are some
        where it is not enough and we have to use `Type.toString()`. For example, class type `java.lang.Number`
        and wildcard type `? extends java.lang.Number` have the same `name()`. We only use the `toString()` form
        to establish some kind of deterministic ordering, nothing more.
        """)
public class Reproducibility {
    static final Comparator<BeanInfo> BEAN_COMPARATOR = Comparator.comparing(BeanInfo::getIdentifier);
    static final Comparator<ObserverInfo> OBSERVER_COMPARATOR = Comparator.comparing(ObserverInfo::getIdentifier);
    static final Comparator<Type> TYPE_COMPARATOR = Comparator.comparing(Type::name).thenComparing(Type::toString);
    static final Comparator<ClassInfo> CLASS_COMPARATOR = Comparator.comparing(c -> c.name().toString());
    static final Comparator<AnnotationInstance> ANNOTATION_COMPARATOR = Comparator.comparing(AnnotationInstance::name)
            .thenComparing(it -> it.toString());
    static final Comparator<AnnotationInstanceEquivalenceProxy> ANNOTATION_PROXY_COMPARATOR = Comparator
            .<AnnotationInstanceEquivalenceProxy, DotName> comparing(it -> it.get().name())
            .thenComparing(it -> it.get().toString());
    static final Comparator<StereotypeInfo> STEREOTYPE_COMPARATOR = Comparator.comparing(StereotypeInfo::getName);
    static final Comparator<InjectionPointInfo> INJECTION_POINT_COMPARATOR = Comparator
            .comparing(InjectionPointInfo::getType, TYPE_COMPARATOR)
            .thenComparing(InjectionPointInfo::getRequiredQualifiers,
                    setComparator(ANNOTATION_COMPARATOR, AnnotationInstance[]::new));
    static final Comparator<MethodInfo> METHOD_COMPARATOR = Comparator
            .comparing(MethodInfo::name)
            .thenComparing(MethodInfo::parameterTypes, listComparator(TYPE_COMPARATOR))
            .thenComparing(MethodInfo::returnType, TYPE_COMPARATOR);

    // this is relatively sane only for tiny sets, which is the case here
    private static <T> Comparator<Set<T>> setComparator(Comparator<T> comparator, IntFunction<T[]> toArray) {
        return (a, b) -> {
            T[] aArray = a.toArray(toArray);
            T[] bArray = b.toArray(toArray);
            Arrays.sort(aArray, comparator);
            Arrays.sort(bArray, comparator);
            int len = Math.min(aArray.length, bArray.length);
            for (int i = 0; i < len; i++) {
                int cmp = comparator.compare(aArray[i], bArray[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(aArray.length, bArray.length);
        };
    }

    // this is relatively sane only for tiny lists, which is the case here
    private static <T> Comparator<List<T>> listComparator(Comparator<T> comparator) {
        return (a, b) -> {
            int len = Math.min(a.size(), b.size());
            for (int i = 0; i < len; i++) {
                int cmp = comparator.compare(a.get(i), b.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(a.size(), b.size());
        };
    }

    static List<BeanInfo> orderedBeans(Collection<BeanInfo> beans) {
        return ordered(beans, BEAN_COMPARATOR);
    }

    static List<InterceptorInfo> orderedInterceptors(Collection<InterceptorInfo> interceptors) {
        return ordered(interceptors, BEAN_COMPARATOR);
    }

    static List<DecoratorInfo> orderedDecorators(Collection<DecoratorInfo> decorators) {
        return ordered(decorators, BEAN_COMPARATOR);
    }

    static List<ObserverInfo> orderedObservers(Collection<ObserverInfo> observers) {
        return ordered(observers, OBSERVER_COMPARATOR);
    }

    static List<Type> orderedTypes(Collection<Type> types) {
        return ordered(types, TYPE_COMPARATOR);
    }

    static List<AnnotationInstance> orderedAnnotations(Collection<AnnotationInstance> annotations) {
        return ordered(annotations, ANNOTATION_COMPARATOR);
    }

    static List<AnnotationInstanceEquivalenceProxy> orderedAnnotationProxies(
            Collection<AnnotationInstanceEquivalenceProxy> annotationProxies) {
        return ordered(annotationProxies, ANNOTATION_PROXY_COMPARATOR);
    }

    static List<StereotypeInfo> orderedStereotypes(Collection<StereotypeInfo> stereotypes) {
        return ordered(stereotypes, STEREOTYPE_COMPARATOR);
    }

    static List<InjectionPointInfo> orderedInjectionPoints(Collection<InjectionPointInfo> injectionPoints) {
        return ordered(injectionPoints, INJECTION_POINT_COMPARATOR);
    }

    static List<ClassInfo> orderedClasses(Collection<ClassInfo> classes) {
        return ordered(classes, CLASS_COMPARATOR);
    }

    static List<MethodInfo> orderedMethods(Collection<MethodInfo> methods) {
        return ordered(methods, METHOD_COMPARATOR);
    }

    private static <T> List<T> ordered(Collection<T> collection, Comparator<? super T> comparator) {
        if (collection.isEmpty()) {
            return List.of();
        }
        if (collection.size() == 1) {
            return List.of(collection.iterator().next());
        }
        List<T> list = new ArrayList<>(collection);
        list.sort(comparator);
        return list;
    }
}
