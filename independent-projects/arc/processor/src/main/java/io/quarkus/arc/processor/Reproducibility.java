package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.DotName;
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
    static final Comparator<AnnotationInstance> ANNOTATION_COMPARATOR = Comparator.comparing(AnnotationInstance::name)
            .thenComparing(it -> it.toString());
    static final Comparator<AnnotationInstanceEquivalenceProxy> ANNOTATION_PROXY_COMPARATOR = Comparator
            .<AnnotationInstanceEquivalenceProxy, DotName> comparing(it -> it.get().name())
            .thenComparing(it -> it.get().toString());
    static final Comparator<StereotypeInfo> STEREOTYPE_COMPARATOR = Comparator.comparing(StereotypeInfo::getName);

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
