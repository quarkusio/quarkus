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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.protean.arc.processor.Methods.MethodKey;
import org.jboss.protean.gizmo.MethodCreator;

/**
 *
 * @author Martin Kouba
 */
class BeanInfo {

    private final ClassInfo implClazz;

    private final Optional<AnnotationTarget> target;

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

    // Gizmo consumers are only used by synthetic beans

    private final Consumer<MethodCreator> creatorConsumer;

    private final Consumer<MethodCreator> destroyerConsumer;

    private final Map<String, Object> params;

    BeanInfo(AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope, Set<Type> types, Set<AnnotationInstance> qualifiers,
            List<Injection> injections, BeanInfo declaringBean, DisposerInfo disposer, Integer alternativePriority, List<StereotypeInfo> stereotypes) {
        this(null, target, beanDeployment, scope, types, qualifiers, injections, declaringBean, disposer, alternativePriority, stereotypes, null, null,
                Collections.emptyMap());
    }

    BeanInfo(ClassInfo implClazz, AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope, Set<Type> types, Set<AnnotationInstance> qualifiers,
            List<Injection> injections, BeanInfo declaringBean, DisposerInfo disposer, Integer alternativePriority, List<StereotypeInfo> stereotypes,
            Consumer<MethodCreator> creatorConsumer, Consumer<MethodCreator> destroyerConsumer, Map<String, Object> params) {
        this.target = Optional.ofNullable(target);
        if (implClazz == null && target != null) {
            switch (target.kind()) {
                case CLASS:
                    implClazz = target.asClass();
                    break;
                case FIELD:
                    implClazz = beanDeployment.getIndex().getClassByName(target.asField().type().name());
                    break;
                case METHOD:
                    implClazz = beanDeployment.getIndex().getClassByName(target.asMethod().returnType().name());
                    break;
                default:
                    break;
            }
        }
        this.implClazz = implClazz;
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
        this.creatorConsumer = creatorConsumer;
        this.destroyerConsumer = destroyerConsumer;
        this.params = params;
    }

    Optional<AnnotationTarget> getTarget() {
        return target;
    }

    ClassInfo getImplClazz() {
        return implClazz;
    }

    boolean isClassBean() {
        return target.isPresent() && Kind.CLASS.equals(target.get().kind());
    }

    boolean isProducerMethod() {
        return target.isPresent() && Kind.METHOD.equals(target.get().kind());
    }

    boolean isProducerField() {
        return target.isPresent() && Kind.FIELD.equals(target.get().kind());
    }

    boolean isSynthetic() {
        return !target.isPresent();
    }

    DotName getBeanClass() {
        if (declaringBean != null) {
            return declaringBean.implClazz.name();
        }
        return implClazz.name();
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
        if (target.isPresent()) {
            switch (target.get().kind()) {
                case CLASS:
                    return Types.getProviderType(target.get().asClass());
                case FIELD:
                    return target.get().asField().type();
                case METHOD:
                    return target.get().asMethod().returnType();
                default:
                    break;
            }
        } else if (implClazz != null) {
            return Type.create(implClazz.name(), org.jboss.jandex.Type.Kind.CLASS);
        }
        throw new IllegalStateException("Cannot infer the provider type");
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
                    && Beans.getCallbacks(target.get().asClass(), DotNames.PRE_DESTROY, beanDeployment.getIndex()).isEmpty();
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

    Consumer<MethodCreator> getCreatorConsumer() {
        return creatorConsumer;
    }

    Consumer<MethodCreator> getDestroyerConsumer() {
        return destroyerConsumer;
    }

    Map<String, Object> getParams() {
        return params;
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
        if (isProducerMethod()) {
            return "PRODUCER METHOD";
        } else if (isProducerField()) {
            return "PRODUCER FIELD";
        } else if (isSynthetic()) {
            return "SYNTHETIC";
        } else {
            return target.get().kind().toString();
        }
    }

    private Map<MethodInfo, InterceptionInfo> initInterceptedMethods() {
        if (!isInterceptor() && isClassBean()) {
            Map<MethodInfo, InterceptionInfo> interceptedMethods = new HashMap<>();
            Map<MethodKey, Set<AnnotationInstance>> candidates = new HashMap<>();
            // TODO interceptor bindings are transitive!!!

            List<AnnotationInstance> classLevelBindings = new ArrayList<>();
            addClassLevelBindings(target.get().asClass(), classLevelBindings);
            if (!stereotypes.isEmpty()) {
                for (StereotypeInfo stereotype : stereotypes) {
                    addClassLevelBindings(stereotype.getTarget(), classLevelBindings);
                }
            }

            Methods.addInterceptedMethodCandidates(beanDeployment, target.get().asClass(), candidates, classLevelBindings);

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
            addClassLevelBindings(target.get().asClass(), classLevelBindings);
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
        StringBuilder builder = new StringBuilder();
        builder.append(getType());
        builder.append(" bean [types=");
        builder.append(types);
        builder.append(", qualifiers=");
        builder.append(qualifiers);
        builder.append(", target=");
        builder.append(target);
        if (declaringBean != null) {
            builder.append(", declaringBean=");
            builder.append(declaringBean.target);
        }
        builder.append("]");
        return builder.toString();
    }

    static class Builder {

        private ClassInfo implClazz;

        private AnnotationTarget target;

        private BeanDeployment beanDeployment;

        private ScopeInfo scope;

        private Set<Type> types;

        private Set<AnnotationInstance> qualifiers;

        private List<Injection> injections;

        private BeanInfo declaringBean;

        private DisposerInfo disposer;

        private Integer alternativePriority;

        private List<StereotypeInfo> stereotypes;

        private Consumer<MethodCreator> creatorConsumer;

        private Consumer<MethodCreator> destroyerConsumer;

        private Map<String, Object> params;

        Builder() {
            injections = Collections.emptyList();
            stereotypes = Collections.emptyList();
        }

        Builder implClazz(ClassInfo implClazz) {
            this.implClazz = implClazz;
            return this;
        }

        Builder beanDeployment(BeanDeployment beanDeployment) {
            this.beanDeployment = beanDeployment;
            return this;
        }

        Builder target(AnnotationTarget target) {
            this.target = target;
            return this;
        }

        Builder scope(ScopeInfo scope) {
            this.scope = scope;
            return this;
        }

        Builder types(Set<Type> types) {
            this.types = types;
            return this;
        }

        Builder qualifiers(Set<AnnotationInstance> qualifiers) {
            this.qualifiers = qualifiers;
            return this;
        }

        Builder injections(List<Injection> injections) {
            this.injections = injections;
            return this;
        }

        Builder declaringBean(BeanInfo declaringBean) {
            this.declaringBean = declaringBean;
            return this;
        }

        Builder disposer(DisposerInfo disposer) {
            this.disposer = disposer;
            return this;
        }

        Builder alternativePriority(Integer alternativePriority) {
            this.alternativePriority = alternativePriority;
            return this;
        }

        Builder stereotypes(List<StereotypeInfo> stereotypes) {
            this.stereotypes = stereotypes;
            return this;
        }

        Builder creator(Consumer<MethodCreator> creatorConsumer) {
            this.creatorConsumer = creatorConsumer;
            return this;
        }

        Builder destroyer(Consumer<MethodCreator> destroyerConsumer) {
            this.destroyerConsumer = destroyerConsumer;
            return this;
        }

        Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        BeanInfo build() {
            return new BeanInfo(implClazz, target, beanDeployment, scope, types, qualifiers, injections, declaringBean, disposer, alternativePriority,
                    stereotypes, creatorConsumer, destroyerConsumer, params);
        }

    }

}