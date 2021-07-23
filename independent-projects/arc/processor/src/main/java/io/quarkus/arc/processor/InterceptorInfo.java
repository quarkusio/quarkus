package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.InterceptionType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

/**
 *
 * @author Martin Kouba
 */
public class InterceptorInfo extends BeanInfo implements Comparable<InterceptorInfo> {

    private static final Logger LOGGER = Logger.getLogger(InterceptorInfo.class);

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
        List<MethodInfo> aroundInvokes = new ArrayList<>();
        List<MethodInfo> aroundConstructs = new ArrayList<>();
        List<MethodInfo> postConstructs = new ArrayList<>();
        List<MethodInfo> preDestroys = new ArrayList<>();

        ClassInfo aClass = target.asClass();
        while (aClass != null) {
            for (MethodInfo method : aClass.methods()) {
                if (Modifier.isStatic(method.flags())) {
                    continue;
                }
                if (method.hasAnnotation(DotNames.AROUND_INVOKE)) {
                    aroundInvokes.add(validateSignature(method));
                }
                if (method.hasAnnotation(DotNames.AROUND_CONSTRUCT)) {
                    aroundConstructs.add(validateSignature(method));
                }
                if (method.hasAnnotation(DotNames.POST_CONSTRUCT)) {
                    postConstructs.add(validateSignature(method));
                }
                if (method.hasAnnotation(DotNames.PRE_DESTROY)) {
                    preDestroys.add(validateSignature(method));
                }
            }

            DotName superTypeName = aClass.superName();
            aClass = superTypeName == null || DotNames.OBJECT.equals(superTypeName) ? null
                    : getClassByName(beanDeployment.getBeanArchiveIndex(), superTypeName);
        }

        this.aroundInvoke = aroundInvokes.isEmpty() ? null : aroundInvokes.get(0);
        this.aroundConstruct = aroundConstructs.isEmpty() ? null : aroundConstructs.get(0);
        this.postConstruct = postConstructs.isEmpty() ? null : postConstructs.get(0);
        this.preDestroy = preDestroys.isEmpty() ? null : preDestroys.get(0);
        if (aroundConstruct == null && aroundInvoke == null && preDestroy == null && postConstruct == null) {
            LOGGER.warnf("%s declares no around-invoke method nor a lifecycle callback!", this);
        }
    }

    private MethodInfo validateSignature(MethodInfo method) {
        List<Type> parameters = method.parameters();
        if (parameters.size() != 1 || !(parameters.get(0).name().equals(DotNames.INVOCATION_CONTEXT)
                || parameters.get(0).name().equals(DotNames.ARC_INVOCATION_CONTEXT))) {
            throw new IllegalStateException(
                    "An interceptor method must accept exactly one parameter of type javax.interceptor.InvocationContext: "
                            + method + " declared on " + method.declaringClass());
        }
        if (!method.returnType().kind().equals(Type.Kind.VOID) &&
                !method.returnType().name().equals(DotNames.OBJECT)) {
            throw new IllegalStateException(
                    "The return type of an interceptor method must be java.lang.Object or void: "
                            + method + " declared on " + method.declaringClass());
        }
        return method;
    }

    public Set<AnnotationInstance> getBindings() {
        return bindings;
    }

    public int getPriority() {
        return priority;
    }

    public MethodInfo getAroundInvoke() {
        return aroundInvoke;
    }

    public MethodInfo getAroundConstruct() {
        return aroundConstruct;
    }

    public MethodInfo getPostConstruct() {
        return postConstruct;
    }

    public MethodInfo getPreDestroy() {
        return preDestroy;
    }

    public boolean intercepts(InterceptionType interceptionType) {
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
