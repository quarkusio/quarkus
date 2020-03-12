package io.quarkus.arc.processor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.InterceptionType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

/**
 *
 * @author Martin Kouba
 */
class InterceptorInfo extends BeanInfo implements Comparable<InterceptorInfo> {

    private final Set<AnnotationInstance> bindings;

    private final MethodInfo aroundInvoke;

    private final MethodInfo aroundConstruct;

    private final MethodInfo postConstruct;

    private final MethodInfo preDestroy;

    private final int priority;

    /**
     *
     * @param target
     * @param beanDeployment
     * @param bindings
     * @param injections
     */
    InterceptorInfo(AnnotationTarget target, BeanDeployment beanDeployment, Set<AnnotationInstance> bindings,
            List<Injection> injections, int priority) {
        super(target, beanDeployment, BuiltinScope.DEPENDENT.getInfo(),
                Collections.singleton(Type.create(target.asClass().name(), Kind.CLASS)), new HashSet<>(), injections,
                null, null, null, Collections.emptyList(), null, false);
        this.bindings = bindings;
        this.priority = priority;
        MethodInfo aroundInvoke = null;
        MethodInfo aroundConstruct = null;
        MethodInfo postConstruct = null;
        MethodInfo preDestroy = null;
        for (MethodInfo method : target.asClass().methods()) {
            if (aroundInvoke == null && method.hasAnnotation(DotNames.AROUND_INVOKE)) {
                aroundInvoke = method;
            } else if (method.hasAnnotation(DotNames.AROUND_CONSTRUCT)) {
                // validate compliance with rules for AroundConstruct methods
                if (!method.parameters().equals(Collections.singletonList(
                        Type.create(DotName.createSimple("javax.interceptor.InvocationContext"), Type.Kind.CLASS)))) {
                    throw new IllegalStateException(
                            "@AroundConstruct must have exactly one argument of type javax.interceptor.InvocationContext, but method "
                                    + method.asMethod() + " declared by " + method.declaringClass()
                                    + " violates this.");
                }
                if (!method.returnType().kind().equals(Type.Kind.VOID) &&
                        !method.returnType().name().equals(DotNames.OBJECT)) {
                    throw new IllegalStateException("Return type of @AroundConstruct method must be Object or void, but method "
                            + method.asMethod() + " declared by " + method.declaringClass()
                            + " violates this.");
                }
                aroundConstruct = method;
            } else if (postConstruct == null && method.hasAnnotation(DotNames.POST_CONSTRUCT)) {
                postConstruct = method;
            } else if (preDestroy == null && method.hasAnnotation(DotNames.PRE_DESTROY)) {
                preDestroy = method;
            }
        }
        this.aroundInvoke = aroundInvoke;
        this.aroundConstruct = aroundConstruct;
        this.postConstruct = postConstruct;
        this.preDestroy = preDestroy;
    }

    Set<AnnotationInstance> getBindings() {
        return bindings;
    }

    int getPriority() {
        return priority;
    }

    MethodInfo getAroundInvoke() {
        return aroundInvoke;
    }

    MethodInfo getAroundConstruct() {
        return aroundConstruct;
    }

    MethodInfo getPostConstruct() {
        return postConstruct;
    }

    MethodInfo getPreDestroy() {
        return preDestroy;
    }

    boolean intercepts(InterceptionType interceptionType) {
        switch (interceptionType) {
            case AROUND_INVOKE:
                return aroundInvoke != null;
            case AROUND_CONSTRUCT:
                return aroundConstruct != null;
            case POST_CONSTRUCT:
                return postConstruct != null;
            case PRE_DESTROY:
                return preDestroy != null;
            default:
                return false;
        }
    }

    @Override
    public boolean isInterceptor() {
        return true;
    }

    @Override
    public String toString() {
        return "INTERCEPTOR bean [bindings=" + bindings + ", target=" + getTarget() + "]";
    }

    @Override
    public int compareTo(InterceptorInfo other) {
        return getTarget().toString().compareTo(other.getTarget().toString());
    }

}
