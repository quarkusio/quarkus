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

import javax.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
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

    private final DisposerInfo disposer;

    private Map<MethodInfo, InterceptionInfo> interceptedMethods;

    private Map<InterceptionType, InterceptionInfo> lifecycleInterceptors;

    private final Integer alternativePriority;

    private final List<StereotypeInfo> stereotypes;

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
     * @param disposer
     * @param alternativePriority
     * @param stereotypes
     */
    BeanInfo(AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope, Set<Type> types, Set<AnnotationInstance> qualifiers,
            List<Injection> injections, BeanInfo declaringBean, DisposerInfo disposer, Integer alternativePriority, List<StereotypeInfo> stereotypes) {
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
        this.disposer = disposer;
        this.alternativePriority = alternativePriority;
        this.stereotypes = stereotypes;
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

    Map<MethodInfo, InterceptionInfo> getInterceptedMethods() {
        return interceptedMethods;
    }

    InterceptionInfo getLifecycleInterceptors(InterceptionType interceptionType) {
        return lifecycleInterceptors.containsKey(interceptionType) ? lifecycleInterceptors.get(interceptionType) : InterceptionInfo.EMPTY;
    }

    boolean hasLifecycleInterceptors() {
        return !lifecycleInterceptors.isEmpty();
    }

    boolean isSubclassRequired() {
        return !interceptedMethods.isEmpty() || lifecycleInterceptors.containsKey(InterceptionType.PRE_DESTROY);
    }

    boolean hasDefaultDestroy() {
        if (isInterceptor()) {
            return true;
        }
        if (isClassBean()) {
            return getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()
                    && Beans.getCallbacks(target.asClass(), DotNames.PRE_DESTROY, beanDeployment.getIndex()).isEmpty();
        } else {
            return disposer == null;
        }
    }

    /**
     *
     * @return an ordered list of all interceptors associated with the bean
     */
    List<InterceptorInfo> getBoundInterceptors() {
        List<InterceptorInfo> bound = new ArrayList<>();
        for (InterceptionInfo interception : lifecycleInterceptors.values()) {
            bound.addAll(interception.interceptors);
        }
        if (!interceptedMethods.isEmpty()) {
            bound.addAll(interceptedMethods.values().stream().map(m -> m.interceptors).flatMap(list -> list.stream()).collect(Collectors.toList()));
        }
        return bound.isEmpty() ? Collections.emptyList() : bound.stream().distinct().sorted().collect(Collectors.toList());
    }

    DisposerInfo getDisposer() {
        return disposer;
    }

    boolean isAlternative() {
        return alternativePriority != null;
    }

    Integer getAlternativePriority() {
        return alternativePriority;
    }

    List<StereotypeInfo> getStereotypes() {
        return stereotypes;
    }

    void init() {
        for (Injection injection : injections) {
            for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                Beans.resolveInjectionPoint(beanDeployment, this, injectionPoint);
            }
        }
        if (disposer != null) {
            disposer.init();
        }
        interceptedMethods = initInterceptedMethods();
        lifecycleInterceptors = initLifecycleInterceptors();
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

    private Map<MethodInfo, InterceptionInfo> initInterceptedMethods() {
        if (!isInterceptor() && isClassBean()) {
            Map<MethodInfo, InterceptionInfo> interceptedMethods = new HashMap<>();
            Map<MethodKey, Set<AnnotationInstance>> candidates = new HashMap<>();
            // TODO interceptor bindings are transitive!!!

            List<AnnotationInstance> classLevelBindings = new ArrayList<>();
            addClassLevelBindings(target.asClass(), classLevelBindings);
            if (!stereotypes.isEmpty()) {
                for (StereotypeInfo stereotype : stereotypes) {
                    addClassLevelBindings(stereotype.getTarget(), classLevelBindings);
                }
            }

            Methods.addInterceptedMethodCandidates(beanDeployment, target.asClass(), candidates, classLevelBindings);

            for (Entry<MethodKey, Set<AnnotationInstance>> entry : candidates.entrySet()) {
                List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver().resolve(InterceptionType.AROUND_INVOKE, entry.getValue());
                if (!interceptors.isEmpty()) {
                    interceptedMethods.put(entry.getKey().method, new InterceptionInfo(interceptors, entry.getValue()));
                }
            }
            return interceptedMethods;
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<InterceptionType, InterceptionInfo> initLifecycleInterceptors() {
        if (!isInterceptor() && isClassBean()) {
            Map<InterceptionType, InterceptionInfo> lifecycleInterceptors = new HashMap<>();
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

    private void putLifecycleInterceptors(Map<InterceptionType, InterceptionInfo> lifecycleInterceptors, Set<AnnotationInstance> classLevelBindings,
            InterceptionType interceptionType) {
        List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver().resolve(interceptionType, classLevelBindings);
        if (!interceptors.isEmpty()) {
            lifecycleInterceptors.put(interceptionType, new InterceptionInfo(interceptors, classLevelBindings));
        }
    }

    private void addClassLevelBindings(ClassInfo classInfo, Collection<AnnotationInstance> bindings) {
        beanDeployment.getAnnotations(classInfo).stream()
                .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null && bindings.stream().noneMatch(e -> e.name().equals(a.name())))
                .forEach(a -> bindings.add(a));
        if (classInfo.superClassType() != null && !classInfo.superClassType().name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = beanDeployment.getIndex().getClassByName(classInfo.superName());
            if (superClass != null) {
                addClassLevelBindings(superClass, bindings);
            }
        }
    }

    static class InterceptionInfo {

        static final InterceptionInfo EMPTY = new InterceptionInfo(Collections.emptyList(), Collections.emptySet());

        final List<InterceptorInfo> interceptors;

        final Set<AnnotationInstance> bindings;

        InterceptionInfo(List<InterceptorInfo> interceptors, Set<AnnotationInstance> bindings) {
            this.interceptors = interceptors;
            this.bindings = bindings;
        }

        boolean isEmpty() {
            return interceptors.isEmpty();
        }

    }

    @Override
    public String toString() {
        return getType() + " bean [types=" + types + ", qualifiers=" + qualifiers + ", target=" + target + "]";
    }

}