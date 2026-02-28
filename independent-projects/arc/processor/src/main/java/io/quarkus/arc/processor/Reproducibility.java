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
        List<BeanInfo> list = new ArrayList<>(beans);
        list.sort(BEAN_COMPARATOR);
        return list;
    }

    static List<InterceptorInfo> orderedInterceptors(Collection<InterceptorInfo> interceptors) {
        List<InterceptorInfo> list = new ArrayList<>(interceptors);
        list.sort(BEAN_COMPARATOR);
        return list;
    }

    static List<DecoratorInfo> orderedDecorators(Collection<DecoratorInfo> decorators) {
        List<DecoratorInfo> list = new ArrayList<>(decorators);
        list.sort(BEAN_COMPARATOR);
        return list;
    }

    static List<ObserverInfo> orderedObservers(Collection<ObserverInfo> observers) {
        List<ObserverInfo> list = new ArrayList<>(observers);
        list.sort(OBSERVER_COMPARATOR);
        return list;
    }

    static List<Type> orderedTypes(Collection<Type> types) {
        List<Type> list = new ArrayList<>(types);
        list.sort(TYPE_COMPARATOR);
        return list;
    }

    static List<AnnotationInstance> orderedAnnotations(Collection<AnnotationInstance> annotations) {
        List<AnnotationInstance> list = new ArrayList<>(annotations);
        list.sort(ANNOTATION_COMPARATOR);
        return list;
    }

    static List<AnnotationInstanceEquivalenceProxy> orderedAnnotationProxies(
            Collection<AnnotationInstanceEquivalenceProxy> annotationProxies) {
        List<AnnotationInstanceEquivalenceProxy> list = new ArrayList<>(annotationProxies);
        list.sort(ANNOTATION_PROXY_COMPARATOR);
        return list;
    }

    static List<StereotypeInfo> orderedStereotypes(Collection<StereotypeInfo> stereotypes) {
        List<StereotypeInfo> list = new ArrayList<>(stereotypes);
        list.sort(STEREOTYPE_COMPARATOR);
        return list;
    }
}
