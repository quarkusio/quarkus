package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkus.arc.InterceptorCreator;
import io.quarkus.arc.InterceptorCreator.InterceptFunction;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.Sets;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class InterceptorInfo extends BeanInfo implements Comparable<InterceptorInfo> {

    private static final Logger LOGGER = Logger.getLogger(InterceptorInfo.class);

    private final Set<AnnotationInstance> bindings;
    private final List<MethodInfo> aroundInvokes;
    private final List<MethodInfo> aroundConstructs;
    private final List<MethodInfo> postConstructs;
    private final List<MethodInfo> preDestroys;
    // These fields are only used for synthetic interceptors
    private final InterceptionType interceptionType;
    private final Class<? extends InterceptorCreator> creatorClass;

    InterceptorInfo(Class<? extends InterceptorCreator> creatorClass, BeanDeployment beanDeployment,
            Set<AnnotationInstance> bindings, List<Injection> injections, int priority, InterceptionType interceptionType,
            Map<String, Object> params, String identifier) {
        super(null, ClassType.create(InterceptFunction.class), null, beanDeployment, BuiltinScope.DEPENDENT.getInfo(),
                Sets.singletonHashSet(Type.create(DotName.OBJECT_NAME, Kind.CLASS)), new HashSet<>(), injections, null,
                null, false,
                Collections.emptyList(), null, false, mc -> {
                    ResultHandle creatorHandle = mc.newInstance(MethodDescriptor.ofConstructor(creatorClass));
                    ResultHandle ret = mc.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(InterceptorCreator.class, "create", InterceptFunction.class,
                                    SyntheticCreationalContext.class),
                            creatorHandle, mc.getMethodParam(0));
                    mc.ifNull(ret).trueBranch().throwException(IllegalStateException.class,
                            creatorClass.getName() + "#create() must not return null");
                    mc.returnValue(ret);
                },
                null, params, true, false, null, priority, creatorClass.getName() + (identifier != null ? identifier : ""),
                null, null, null, null);
        this.bindings = bindings;
        this.interceptionType = interceptionType;
        this.creatorClass = creatorClass;
        this.aroundInvokes = List.of();
        this.aroundConstructs = List.of();
        this.postConstructs = List.of();
        this.preDestroys = List.of();
    }

    InterceptorInfo(AnnotationTarget target, BeanDeployment beanDeployment, Set<AnnotationInstance> bindings,
            List<Injection> injections, int priority) {
        super(target, beanDeployment, BuiltinScope.DEPENDENT.getInfo(),
                Sets.singletonHashSet(Type.create(target.asClass().name(), Kind.CLASS)), new HashSet<>(), injections,
                null, null, false, Collections.emptyList(), null, false, null, priority, null, null);
        this.bindings = bindings;
        this.interceptionType = null;
        this.creatorClass = null;
        AnnotationStore store = beanDeployment.getAnnotationStore();
        List<MethodInfo> aroundInvokes = new ArrayList<>();
        List<MethodInfo> aroundConstructs = new ArrayList<>();
        List<MethodInfo> postConstructs = new ArrayList<>();
        List<MethodInfo> preDestroys = new ArrayList<>();

        List<MethodInfo> allMethods = new ArrayList<>();
        ClassInfo aClass = target.asClass();
        while (aClass != null) {
            // Only one interceptor method of a given type may be declared on a given class
            int aroundInvokesFound = 0, aroundConstructsFound = 0, postConstructsFound = 0, preDestroysFound = 0;

            for (MethodInfo method : aClass.methods()) {
                if (Modifier.isStatic(method.flags())) {
                    continue;
                }
                if (store.hasAnnotation(method, DotNames.PRODUCES) || store.hasAnnotation(method, DotNames.DISPOSES)) {
                    // according to spec, finding @Produces or @Disposes on a method is a DefinitionException
                    throw new DefinitionException(
                            "An interceptor method cannot be marked @Produces or @Disposes - " + method + " in class: "
                                    + aClass);
                }
                if (store.hasAnnotation(method, DotNames.AROUND_INVOKE)) {
                    addInterceptorMethod(allMethods, aroundInvokes, method, InterceptionType.AROUND_INVOKE,
                            InterceptorPlacement.INTERCEPTOR_CLASS);
                    if (++aroundInvokesFound > 1) {
                        throw new DefinitionException(
                                "Multiple @AroundInvoke interceptor methods declared on class: " + aClass);
                    }
                }
                if (store.hasAnnotation(method, DotNames.AROUND_CONSTRUCT)) {
                    addInterceptorMethod(allMethods, aroundConstructs, method, InterceptionType.AROUND_CONSTRUCT,
                            InterceptorPlacement.INTERCEPTOR_CLASS);
                    if (++aroundConstructsFound > 1) {
                        throw new DefinitionException(
                                "Multiple @AroundConstruct interceptor methods declared on class: " + aClass);
                    }
                }
                if (store.hasAnnotation(method, DotNames.POST_CONSTRUCT)) {
                    addInterceptorMethod(allMethods, postConstructs, method, InterceptionType.POST_CONSTRUCT,
                            InterceptorPlacement.INTERCEPTOR_CLASS);
                    if (++postConstructsFound > 1) {
                        throw new DefinitionException(
                                "Multiple @PostConstruct interceptor methods declared on class: " + aClass);
                    }
                }
                if (store.hasAnnotation(method, DotNames.PRE_DESTROY)) {
                    addInterceptorMethod(allMethods, preDestroys, method, InterceptionType.PRE_DESTROY,
                            InterceptorPlacement.INTERCEPTOR_CLASS);
                    if (++preDestroysFound > 1) {
                        throw new DefinitionException(
                                "Multiple @PreDestroy interceptor methods declared on class: " + aClass);
                    }
                }
                allMethods.add(method);
            }

            for (FieldInfo field : aClass.fields()) {
                if (store.hasAnnotation(field, DotNames.PRODUCES)) {
                    // according to spec, finding @Produces on a field is a DefinitionException
                    throw new DefinitionException(
                            "An interceptor field cannot be marked @Produces - " + field + " in class: " + aClass);
                }
            }

            DotName superTypeName = aClass.superName();
            aClass = superTypeName == null || DotNames.OBJECT.equals(superTypeName) ? null
                    : getClassByName(beanDeployment.getBeanArchiveIndex(), superTypeName);
        }

        // The interceptor methods defined by the superclasses are invoked before the interceptor method defined by the interceptor class, most general superclass first.
        Collections.reverse(aroundInvokes);
        Collections.reverse(postConstructs);
        Collections.reverse(preDestroys);
        Collections.reverse(aroundConstructs);

        this.aroundInvokes = List.copyOf(aroundInvokes);
        this.aroundConstructs = List.copyOf(aroundConstructs);
        this.postConstructs = List.copyOf(postConstructs);
        this.preDestroys = List.copyOf(preDestroys);

        if (aroundConstructs.isEmpty() && aroundInvokes.isEmpty() && preDestroys.isEmpty() && postConstructs.isEmpty()) {
            LOGGER.warnf("%s declares no around-invoke method nor a lifecycle callback!", this);
        }
    }

    public Set<AnnotationInstance> getBindings() {
        return bindings;
    }

    InterceptionType getInterceptionType() {
        return interceptionType;
    }

    Class<? extends InterceptorCreator> getCreatorClass() {
        return creatorClass;
    }

    /**
     * Returns all methods annotated with {@link jakarta.interceptor.AroundInvoke} found in the hierarchy of the interceptor
     * class.
     * <p>
     * The returned list is sorted. The method declared on the most general superclass is first. The method declared on the
     * interceptor class is last.
     *
     * @return the interceptor methods
     */
    public List<MethodInfo> getAroundInvokes() {
        return aroundInvokes;
    }

    /**
     * Returns all methods annotated with {@link jakarta.interceptor.AroundConstruct} found in the hierarchy of the interceptor
     * class.
     * <p>
     * The returned list is sorted. The method declared on the most general superclass is first. The method declared on the
     * interceptor class is last.
     *
     * @return the interceptor methods
     */
    public List<MethodInfo> getAroundConstructs() {
        return aroundConstructs;
    }

    /**
     * Returns all methods annotated with {@link jakarta.annotation.PostConstruct} found in the hierarchy of the interceptor
     * class.
     * <p>
     * The returned list is sorted. The method declared on the most general superclass is first. The method declared on the
     * interceptor class is last.
     *
     * @return the interceptor methods
     */
    public List<MethodInfo> getPostConstructs() {
        return postConstructs;
    }

    /**
     * Returns all methods annotated with {@link jakarta.annotation.PreDestroy} found in the hierarchy of the interceptor class.
     * <p>
     * The returned list is sorted. The method declared on the most general superclass is first. The method declared on the
     * interceptor class is last.
     *
     * @return the interceptor methods
     */
    public List<MethodInfo> getPreDestroys() {
        return preDestroys;
    }

    /**
     *
     * @deprecated Use {@link #getAroundInvokes()} instead
     */
    @Deprecated(since = "3.1", forRemoval = true)
    public MethodInfo getAroundInvoke() {
        return aroundInvokes.get(aroundInvokes.size() - 1);
    }

    /**
     *
     * @deprecated Use {@link #getAroundConstructs()} instead
     */
    @Deprecated(since = "3.1", forRemoval = true)
    public MethodInfo getAroundConstruct() {
        return aroundConstructs.get(aroundConstructs.size() - 1);
    }

    /**
     *
     * @deprecated Use {@link #getPostConstructs()} instead
     */
    @Deprecated(since = "3.1", forRemoval = true)
    public MethodInfo getPostConstruct() {
        return postConstructs.get(postConstructs.size() - 1);
    }

    /**
     *
     * @deprecated Use {@link #getPreDestroys()} instead
     */
    @Deprecated(since = "3.1", forRemoval = true)
    public MethodInfo getPreDestroy() {
        return preDestroys.get(preDestroys.size() - 1);
    }

    public boolean intercepts(InterceptionType interceptionType) {
        if (isSynthetic()) {
            return interceptionType == this.interceptionType;
        }
        switch (interceptionType) {
            case AROUND_INVOKE:
                return !aroundInvokes.isEmpty();
            case AROUND_CONSTRUCT:
                return !aroundConstructs.isEmpty();
            case POST_CONSTRUCT:
                return !postConstructs.isEmpty();
            case PRE_DESTROY:
                return !preDestroys.isEmpty();
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

    static void addInterceptorMethod(List<MethodInfo> allMethods, List<MethodInfo> interceptorMethods, MethodInfo method,
            InterceptionType interceptionType, InterceptorPlacement interceptorPlacement) {
        validateSignature(method, interceptionType, interceptorPlacement);
        if (!isInterceptorMethodOverriden(allMethods, method)) {
            interceptorMethods.add(method);
        }
    }

    static boolean isInterceptorMethodOverriden(Iterable<MethodInfo> allMethods, MethodInfo method) {
        for (MethodInfo m : allMethods) {
            if (m.name().equals(method.name()) && hasInterceptorMethodParameter(m)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasInterceptorMethodParameter(MethodInfo method) {
        return method.parametersCount() == 1
                && (method.parameterType(0).name().equals(DotNames.INVOCATION_CONTEXT)
                        || method.parameterType(0).name().equals(DotNames.ARC_INVOCATION_CONTEXT));
    }

    private enum InterceptorMethodError {
        MUST_HAVE_PARAMETER,
        MUST_NOT_HAVE_PARAMETER,
        WRONG_RETURN_TYPE,
    }

    static void validateSignature(MethodInfo method, InterceptionType interceptionType,
            InterceptorPlacement interceptorPlacement) {
        boolean isLifecycleCallback = interceptionType == InterceptionType.AROUND_CONSTRUCT
                || interceptionType == InterceptionType.POST_CONSTRUCT
                || interceptionType == InterceptionType.PRE_DESTROY;

        boolean mustHaveParameter = !isLifecycleCallback || interceptorPlacement == InterceptorPlacement.INTERCEPTOR_CLASS;
        boolean mustNotHaveParameter = isLifecycleCallback && interceptorPlacement == InterceptorPlacement.TARGET_CLASS;
        boolean mayReturnVoid = isLifecycleCallback;
        boolean mayReturnObject = !isLifecycleCallback || interceptorPlacement == InterceptorPlacement.INTERCEPTOR_CLASS;

        Set<InterceptorMethodError> errors = EnumSet.noneOf(InterceptorMethodError.class);
        if (mustHaveParameter && !hasInterceptorMethodParameter(method)) {
            errors.add(InterceptorMethodError.MUST_HAVE_PARAMETER);
        }
        if (mustNotHaveParameter && method.parametersCount() > 0) {
            errors.add(InterceptorMethodError.MUST_NOT_HAVE_PARAMETER);
        }

        boolean wrongReturnType = true;
        if (mayReturnVoid && method.returnType().kind().equals(Kind.VOID)) {
            wrongReturnType = false;
        }
        if (mayReturnObject && method.returnType().name().equals(DotNames.OBJECT)) {
            wrongReturnType = false;
        }
        if (wrongReturnType) {
            errors.add(InterceptorMethodError.WRONG_RETURN_TYPE);
        }

        if (!errors.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            switch (interceptionType) {
                case AROUND_CONSTRUCT:
                    msg.append("@AroundConstruct");
                    break;
                case AROUND_INVOKE:
                    msg.append("@AroundInvoke");
                    break;
                case POST_CONSTRUCT:
                    msg.append("@PostConstruct");
                    break;
                case PRE_DESTROY:
                    msg.append("@PreDestroy");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown interception type: " + interceptionType);
            }
            if (isLifecycleCallback) {
                msg.append(" lifecycle callback method");
            } else {
                msg.append(" interceptor method");
            }
            msg.append(" declared in ");
            switch (interceptorPlacement) {
                case INTERCEPTOR_CLASS:
                    msg.append("an interceptor class");
                    break;
                case TARGET_CLASS:
                    msg.append("a target class");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown interceptor placement: " + interceptorPlacement);
            }
            msg.append(" must ");

            if (errors.contains(InterceptorMethodError.MUST_HAVE_PARAMETER)) {
                msg.append("have exactly one parameter of type jakarta.interceptor.InvocationContext");
            } else if (errors.contains(InterceptorMethodError.MUST_NOT_HAVE_PARAMETER)) {
                msg.append("have zero parameters");
            }

            if (errors.contains(InterceptorMethodError.WRONG_RETURN_TYPE)) {
                if (errors.contains(InterceptorMethodError.MUST_HAVE_PARAMETER)
                        || errors.contains(InterceptorMethodError.MUST_NOT_HAVE_PARAMETER)) {
                    msg.append(" and must ");
                }
                msg.append("have a return type of ");
                if (mayReturnVoid) {
                    msg.append("void");
                }
                if (mayReturnVoid && mayReturnObject) {
                    msg.append(" or ");
                }
                if (mayReturnObject) {
                    msg.append("java.lang.Object");
                }
            }

            msg.append(": ").append(method).append(" declared in ").append(method.declaringClass().name());

            throw new DefinitionException(msg.toString());
        }
    }

}
