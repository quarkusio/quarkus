package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.protean.arc.processor.Methods.MethodKey;

/**
 *
 * @author Martin Kouba
 */
class BeanInfo {

    private final AnnotationTarget target;

    private final BeanDeployment beanDeployment;

    private final ScopeInfo scope;

    private final Set<Type> types;

    private final Set<AnnotationInstance> qualifiers;

    private final List<Injection> injections;

    private final BeanInfo declaringBean;

    private Map<MethodInfo, List<InterceptorInfo>> interceptedMethods;

    private Map<InterceptionType, List<InterceptorInfo>> lifecycleInterceptors;

    /**
     *
     * @param target
     * @param beanDeployment
     * @param scope
     * @param types
     * @param qualifiers
     * @param alternativePriority
     * @param injections
     * @param declaringBean
     */
    BeanInfo(AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope, Set<Type> types, Set<AnnotationInstance> qualifiers,
            List<Injection> injections, BeanInfo declaringBean) {
        this.target = target;
        this.beanDeployment = beanDeployment;
        this.scope = scope != null ? scope : ScopeInfo.DEPENDENT;
        this.types = types;
        if (qualifiers.isEmpty()) {
            qualifiers.add(BuiltinQualifier.DEFAULT.getInstance());
        }
        qualifiers.add(BuiltinQualifier.ANY.getInstance());
        this.qualifiers = qualifiers;
        this.injections = injections;
        this.declaringBean = declaringBean;
    }

    AnnotationTarget getTarget() {
        return target;
    }

    boolean isClassBean() {
        return Kind.CLASS.equals(target.kind());
    }

    boolean isProducerMethod() {
        return Kind.METHOD.equals(target.kind());
    }

    boolean isProducerField() {
        return Kind.FIELD.equals(target.kind());
    }

    boolean isInterceptor() {
        return false;
    }

    BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    BeanDeployment getDeployment() {
        return beanDeployment;
    }

    Type getProviderType() {
        if (Kind.CLASS.equals(target.kind())) {
            return Types.getProviderType(target.asClass());
        } else if (Kind.METHOD.equals(target.kind())) {
            return target.asMethod().returnType();
        } else if (Kind.FIELD.equals(target.kind())) {
            return target.asField().type();
        }
        throw new IllegalStateException("Cannot infer provider type");
    }

    ScopeInfo getScope() {
        return scope;
    }

    Set<Type> getTypes() {
        return types;
    }

    Set<AnnotationInstance> getQualifiers() {
        return qualifiers;
    }

    boolean hasDefaultQualifiers() {
        return qualifiers.size() == 2 && qualifiers.contains(BuiltinQualifier.DEFAULT.getInstance())
                && qualifiers.contains(BuiltinQualifier.DEFAULT.getInstance());
    }

    List<Injection> getInjections() {
        return injections;
    }

    List<InjectionPointInfo> getAllInjectionPoints() {
        if (injections.isEmpty()) {
            return Collections.emptyList();
        }
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (Injection injection : injections) {
            injectionPoints.addAll(injection.injectionPoints);
        }
        return injectionPoints;
    }

    Optional<Injection> getConstructorInjection() {
        return injections.isEmpty() ? Optional.empty() : injections.stream().filter(i -> i.isConstructor()).findAny();
    }

    Map<MethodInfo, List<InterceptorInfo>> getInterceptedMethods() {
        return interceptedMethods;
    }

    List<InterceptorInfo> getLifecycleInterceptors(InterceptionType interceptionType) {
        return lifecycleInterceptors.containsKey(interceptionType) ? lifecycleInterceptors.get(interceptionType) : Collections.emptyList();
    }

    boolean hasLifecycleInterceptors() {
        return !lifecycleInterceptors.isEmpty();
    }

    boolean isSubclassRequired() {
        return !interceptedMethods.isEmpty() || lifecycleInterceptors.containsKey(InterceptionType.PRE_DESTROY);
    }

    boolean hasDefaultDestroy() {
        // TODO disposer methods
        return isInterceptor() || !isClassBean() || (getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()
                && target.asClass().methods().stream().noneMatch(m -> m.hasAnnotation(DotNames.PRE_DESTROY)));
    }

    /**
     *
     * @return an ordered list of all interceptors associated with the bean
     */
    List<InterceptorInfo> getBoundInterceptors() {
        List<InterceptorInfo> bound = new ArrayList<>();
        for (List<InterceptorInfo> interceptors : lifecycleInterceptors.values()) {
            bound.addAll(interceptors);
        }
        if (!interceptedMethods.isEmpty()) {
            bound.addAll(interceptedMethods.values().stream().flatMap(list -> list.stream()).collect(Collectors.toList()));
        }
        return bound.isEmpty() ? Collections.emptyList() : bound.stream().distinct().sorted().collect(Collectors.toList());
    }

    boolean matches(InjectionPointInfo injectionPoint) {
        // Bean has all the required qualifiers
        for (AnnotationInstance requiredQualifier : injectionPoint.requiredQualifiers) {
            if (!hasQualifier(requiredQualifier)) {
                return false;
            }
        }
        // Bean has a bean type that matches the required type
        return matchesType(injectionPoint.requiredType);
    }

    boolean matchesType(Type requiredType) {
        for (Type beanType : types) {
            if (beanDeployment.getBeanResolver().matches(requiredType, beanType)) {
                return true;
            }
        }
        return false;
    }

    boolean hasQualifier(AnnotationInstance required) {
        ClassInfo requiredInfo = beanDeployment.getQualifier(required.name());
        List<AnnotationValue> binding = required.values().stream().filter(v -> !requiredInfo.method(v.name()).hasAnnotation(DotNames.NONBINDING))
                .collect(Collectors.toList());
        for (AnnotationInstance qualifier : qualifiers) {
            if (required.name().equals(qualifier.name())) {
                // Must have the same annotation member value for each member which is not annotated @Nonbinding
                boolean matches = true;
                for (AnnotationValue value : binding) {
                    if (!value.equals(qualifier.value(value.name()))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
        }
        return false;
    }

    void init() {
        for (Injection injection : injections) {
            for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                resolveInjectionPoint(injectionPoint);
            }
        }
        interceptedMethods = initInterceptedMethods();
        lifecycleInterceptors = initLifecycleInterceptors();
    }

    private void resolveInjectionPoint(InjectionPointInfo injectionPoint) {
        if (BuiltinBean.resolvesTo(injectionPoint)) {
            // Skip built-in beans
            return;
        }
        List<BeanInfo> resolved = new ArrayList<>();
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (bean.matches(injectionPoint)) {
                resolved.add(bean);
            }
        }
        if (resolved.isEmpty()) {
            throw new UnsatisfiedResolutionException(injectionPoint + " on " + this);
        } else if (resolved.size() > 1) {
            throw new AmbiguousResolutionException(
                    injectionPoint + " on " + this + "\nBeans:\n" + resolved.stream().map(Object::toString).collect(Collectors.joining("\n")));
        }
        injectionPoint.resolve(resolved.get(0));
    }

    protected String getType() {
        if (Kind.METHOD.equals(target.kind())) {
            return "PRODUCER METHOD";
        } else if (Kind.FIELD.equals(target.kind())) {
            return "PRODUCER FIELD";
        } else {
            return target.kind().toString();
        }
    }

    private Map<MethodInfo, List<InterceptorInfo>> initInterceptedMethods() {
        if (!isInterceptor() && isClassBean()) {
            Map<MethodInfo, List<InterceptorInfo>> interceptedMethods = new HashMap<>();

            Map<MethodKey, Set<AnnotationInstance>> candidates = new HashMap<>();
            // TODO interceptor bindings are transitive!!!
            List<AnnotationInstance> classLevelBindings = new ArrayList<>();
            addClassLevelBindings(target.asClass(), classLevelBindings);
            Methods.addInterceptedMethodCandidates(beanDeployment, target.asClass(), candidates, classLevelBindings);

            for (Entry<MethodKey, Set<AnnotationInstance>> entry : candidates.entrySet()) {
                List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver().resolve(InterceptionType.AROUND_INVOKE, entry.getValue());
                if (!interceptors.isEmpty()) {
                    interceptedMethods.put(entry.getKey().method, interceptors);
                }
            }
            return interceptedMethods;
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<InterceptionType, List<InterceptorInfo>> initLifecycleInterceptors() {
        if (!isInterceptor() && isClassBean()) {
            Map<InterceptionType, List<InterceptorInfo>> lifecycleInterceptors = new HashMap<>();
            Set<AnnotationInstance> classLevelBindings = new HashSet<>();
            addClassLevelBindings(target.asClass(), classLevelBindings);
            putLifecycleInterceptors(lifecycleInterceptors, classLevelBindings, InterceptionType.POST_CONSTRUCT);
            putLifecycleInterceptors(lifecycleInterceptors, classLevelBindings, InterceptionType.PRE_DESTROY);
            putLifecycleInterceptors(lifecycleInterceptors, classLevelBindings, InterceptionType.AROUND_CONSTRUCT);
            return lifecycleInterceptors;
        } else {
            return Collections.emptyMap();
        }
    }

    private void putLifecycleInterceptors(Map<InterceptionType, List<InterceptorInfo>> lifecycleInterceptors, Set<AnnotationInstance> classLevelBindings,
            InterceptionType interceptionType) {
        List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver().resolve(interceptionType, classLevelBindings);
        if (!interceptors.isEmpty()) {
            lifecycleInterceptors.put(interceptionType, interceptors);
        }
    }

    private void addClassLevelBindings(ClassInfo classInfo, Collection<AnnotationInstance> bindings) {
        classInfo.classAnnotations().stream()
                .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null && bindings.stream().noneMatch(e -> e.name().equals(a.name())))
                .forEach(a -> bindings.add(a));
        if (classInfo.superClassType() != null && !classInfo.superClassType().name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = beanDeployment.getIndex().getClassByName(classInfo.superName());
            if (superClass != null) {
                addClassLevelBindings(superClass, bindings);
            }
        }
    }

    @Override
    public String toString() {
        return getType() + " bean [types=" + types + ", qualifiers=" + qualifiers + ", target=" + target + "]";
    }

}