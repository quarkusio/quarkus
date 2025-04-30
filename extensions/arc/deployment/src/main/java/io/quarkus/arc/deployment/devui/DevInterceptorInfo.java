package io.quarkus.arc.deployment.devui;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.processor.InterceptorInfo;

public class DevInterceptorInfo implements Comparable<DevInterceptorInfo> {

    public static DevInterceptorInfo from(InterceptorInfo interceptor, CompletedApplicationClassPredicateBuildItem predicate) {
        boolean isApplicationBean = predicate.test(interceptor.getBeanClass());
        Set<Name> bindings = new HashSet<>();
        for (AnnotationInstance binding : interceptor.getBindings()) {
            bindings.add(Name.from(binding));
        }
        Set<InterceptionType> intercepts = new HashSet<>();
        if (interceptor.intercepts(InterceptionType.AROUND_INVOKE)) {
            intercepts.add(InterceptionType.AROUND_INVOKE);
        }
        if (interceptor.intercepts(InterceptionType.AROUND_CONSTRUCT)) {
            intercepts.add(InterceptionType.AROUND_CONSTRUCT);
        }
        if (interceptor.intercepts(InterceptionType.POST_CONSTRUCT)) {
            intercepts.add(InterceptionType.POST_CONSTRUCT);
        }
        if (interceptor.intercepts(InterceptionType.PRE_DESTROY)) {
            intercepts.add(InterceptionType.PRE_DESTROY);
        }
        return new DevInterceptorInfo(interceptor.getIdentifier(), Name.from(interceptor.getBeanClass()), bindings,
                interceptor.getPriority(), intercepts,
                isApplicationBean);
    }

    private final String id;
    private final Name interceptorClass;
    private final Set<Name> bindings;
    private final int priority;
    private final Set<InterceptionType> intercepts;
    private final boolean isApplicationBean;

    DevInterceptorInfo(String id, Name interceptorClass, Set<Name> bindings, int priority,
            Set<InterceptionType> intercepts, boolean isApplicationBean) {
        this.id = id;
        this.interceptorClass = interceptorClass;
        this.bindings = bindings;
        this.priority = priority;
        this.intercepts = intercepts;
        this.isApplicationBean = isApplicationBean;
    }

    public String getId() {
        return id;
    }

    public Name getInterceptorClass() {
        return interceptorClass;
    }

    public Set<Name> getBindings() {
        return bindings;
    }

    public int getPriority() {
        return priority;
    }

    public Set<InterceptionType> getIntercepts() {
        return intercepts;
    }

    @Override
    public int compareTo(DevInterceptorInfo o) {
        // Application beans should go first
        if (isApplicationBean == o.isApplicationBean) {
            return interceptorClass.compareTo(o.interceptorClass);
        }
        return isApplicationBean ? -1 : 1;
    }

}
