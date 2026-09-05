package io.quarkus.arc.runtime.produi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the running ArC (CDI) container: the registered
 * beans, observer methods and interceptors, plus the set of supported scopes.
 * <p>
 * The Dev UI is not reused: its {@code qwc-arc-*} components are backed by
 * build-time data and import dev-only web modules ({@code build-time-data},
 * {@code qui-badge}, {@code qui-ide-link}) that the Prod UI bundle does not
 * shim, and it also carries dev-only monitoring (fired events, invocation
 * trees). A bespoke read-only component is provided instead, backed by this
 * service which reads the always-present runtime {@link ArcContainerImpl}.
 * <p>
 * The bean list is filtered to hide the container's built-in plumbing beans
 * (the {@code BUILTIN} kind: {@code BeanManager}, {@code Event},
 * {@code Instance}, etc.) so the view focuses on application and extension
 * beans. Only metadata (class, scope, kind, qualifiers, types) is exposed;
 * no bean instances are created and no state is mutated, so nothing is
 * destructive and no secrets are ever read.
 */
@ApplicationScoped
public class ArcProdUIService {

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the registered CDI beans (built-in plumbing beans are filtered out)")
    public List<BeanInfo> getBeans() {
        List<BeanInfo> beans = new ArrayList<>();
        for (InjectableBean<?> bean : container().getBeans()) {
            if (bean.getKind() == InjectableBean.Kind.BUILTIN) {
                // Hide the container's built-in plumbing beans (BeanManager, Event, Instance, ...)
                continue;
            }
            beans.add(new BeanInfo(
                    className(bean.getBeanClass()),
                    simpleName(bean.getScope()),
                    bean.getKind() == null ? null : bean.getKind().name(),
                    bean.getName(),
                    bean.isAlternative(),
                    bean.isDefaultBean(),
                    typeNames(bean.getTypes()),
                    qualifierNames(bean.getQualifiers())));
        }
        beans.sort(Comparator.comparing(BeanInfo::beanClass, Comparator.nullsLast(Comparator.naturalOrder())));
        return beans;
    }

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the registered CDI observer methods")
    public List<ObserverInfo> getObservers() {
        List<ObserverInfo> observers = new ArrayList<>();
        for (InjectableObserverMethod<?> observer : container().getObservers()) {
            observers.add(new ObserverInfo(
                    className(observer.getBeanClass()),
                    typeName(observer.getObservedType()),
                    qualifierNames(observer.getObservedQualifiers()),
                    observer.getPriority(),
                    observer.isAsync(),
                    observer.getReception() == null ? null : observer.getReception().name(),
                    observer.getTransactionPhase() == null ? null : observer.getTransactionPhase().name()));
        }
        observers.sort(Comparator.comparing(ObserverInfo::observedType, Comparator.nullsLast(Comparator.naturalOrder())));
        return observers;
    }

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the registered CDI interceptors and their bindings")
    public List<InterceptorInfo> getInterceptors() {
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (InjectableInterceptor<?> interceptor : container().getInterceptors()) {
            interceptors.add(new InterceptorInfo(
                    className(interceptor.getBeanClass()),
                    interceptor.getPriority(),
                    qualifierNames(interceptor.getInterceptorBindings())));
        }
        interceptors.sort(Comparator.comparing(InterceptorInfo::interceptorClass,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return interceptors;
    }

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get the set of scopes supported by the running container")
    public List<String> getScopes() {
        return container().getScopes().stream()
                .map(this::simpleName)
                .sorted()
                .collect(Collectors.toList());
    }

    private ArcContainerImpl container() {
        return (ArcContainerImpl) Arc.container();
    }

    private String className(Class<?> clazz) {
        return clazz == null ? null : clazz.getName();
    }

    private String simpleName(Class<?> clazz) {
        return clazz == null ? null : clazz.getSimpleName();
    }

    private String typeName(Type type) {
        return type == null ? null : type.getTypeName();
    }

    private List<String> typeNames(Set<Type> types) {
        if (types == null) {
            return List.of();
        }
        return types.stream().map(this::typeName).sorted().collect(Collectors.toList());
    }

    private List<String> qualifierNames(Set<Annotation> qualifiers) {
        if (qualifiers == null) {
            return List.of();
        }
        return qualifiers.stream()
                .map(q -> "@" + q.annotationType().getSimpleName())
                .sorted()
                .collect(Collectors.toList());
    }

    public record BeanInfo(String beanClass, String scope, String kind, String name, boolean alternative,
            boolean defaultBean, List<String> types, List<String> qualifiers) {
    }

    public record ObserverInfo(String declaringClass, String observedType, List<String> qualifiers, int priority,
            boolean async, String reception, String transactionPhase) {
    }

    public record InterceptorInfo(String interceptorClass, int priority, List<String> bindings) {
    }
}
