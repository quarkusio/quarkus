package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.enterprise.inject.spi.DefinitionException;
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

    private final List<MethodInfo> aroundInvokes;

    private final List<MethodInfo> aroundConstructs;

    private final List<MethodInfo> postConstructs;

    private final List<MethodInfo> preDestroys;


    InterceptorInfo(AnnotationTarget target, BeanDeployment beanDeployment, Set<AnnotationInstance> bindings,
            List<Injection> injections, int priority) {
        super(target, beanDeployment, BuiltinScope.DEPENDENT.getInfo(),
                Collections.singleton(Type.create(target.asClass().name(), Kind.CLASS)), new HashSet<>(), injections,
                null, null, false, Collections.emptyList(), null, false, null, priority);
        this.bindings = bindings;
        aroundInvokes = new ArrayList<>();
        aroundConstructs = new ArrayList<>();
        postConstructs = new ArrayList<>();
        preDestroys = new ArrayList<>();

        ClassInfo aClass = target.asClass();
        processInheritedMethods(aClass, beanDeployment);

        if (aroundConstructs.isEmpty() && aroundInvokes.isEmpty() && preDestroys.isEmpty() && postConstructs.isEmpty()) {
            LOGGER.warnf("%s declares no around-invoke method nor a lifecycle callback!", this);
        }
    }

    private void processInheritedMethods(ClassInfo aClass, BeanDeployment beanDeployment) {
        DotName superTypeName = aClass.superName();
        if (superTypeName != null && !DotNames.OBJECT.equals(superTypeName)) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), superTypeName);
            if (superClassInfo == null) {
                throw new DefinitionException(
                        "The interception super type " + superTypeName + " was not found in the index");
            }
            processInheritedMethods(superClassInfo, beanDeployment);
        }

        for (MethodInfo method : aClass.methods()) {
            if (Modifier.isStatic(method.flags())) {
                continue;
            }
            if (method.hasAnnotation(DotNames.AROUND_INVOKE)) {
                overrideSuperClassMethod(method);
                aroundInvokes.add(validateSignature(method));
            }
            if (method.hasAnnotation(DotNames.AROUND_CONSTRUCT)) {
                overrideSuperClassMethod(method);
                aroundConstructs.add(validateSignature(method));
            }
            if (method.hasAnnotation(DotNames.POST_CONSTRUCT)) {
                overrideSuperClassMethod(method);
                postConstructs.add(validateSignature(method));
            }
            if (method.hasAnnotation(DotNames.PRE_DESTROY)) {
                overrideSuperClassMethod(method);
                preDestroys.add(validateSignature(method));
            }
        }
    }

    private void overrideSuperClassMethod(MethodInfo method) {
        overrideSuperClassMethod(method, aroundInvokes);
        overrideSuperClassMethod(method, aroundConstructs);
        overrideSuperClassMethod(method, postConstructs);
        overrideSuperClassMethod(method, preDestroys);
    }

    private void overrideSuperClassMethod(MethodInfo method, List<MethodInfo> methods) {
        for (ListIterator<MethodInfo> iterator = methods.listIterator(); iterator.hasNext();) {
            MethodInfo interceptorMethod = iterator.next();
            if (method.name().equals(interceptorMethod.name())
                    && method.parameters().size() == 1
                    && method.parameters().get(0).name() == interceptorMethod.parameters().get(0).name()
                    && !method.declaringClass().name().equals(interceptorMethod.declaringClass().name())) {
                iterator.remove();
            }
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

    public List<MethodInfo> getAroundInvokes() {
        return aroundInvokes;
    }

    public List<MethodInfo> getAroundConstructs() {
        return aroundConstructs;
    }

    public List<MethodInfo> getPostConstructs() {
        return postConstructs;
    }

    public List<MethodInfo> getPreDestroys() {
        return preDestroys;
    }

    public boolean intercepts(InterceptionType interceptionType) {
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

}
