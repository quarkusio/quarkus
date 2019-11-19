package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationRule;
import io.quarkus.arc.processor.Methods.MethodKey;
import io.quarkus.gizmo.MethodCreator;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InterceptionType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfo implements InjectionTargetInfo {

    private final String identifier;

    private final ClassInfo implClazz;

    private final Type providerType;

    private final Optional<AnnotationTarget> target;

    private final BeanDeployment beanDeployment;

    private final ScopeInfo scope;

    private final Set<Type> types;

    private final Set<AnnotationInstance> qualifiers;

    private final List<Injection> injections;

    private final BeanInfo declaringBean;

    private final DisposerInfo disposer;

    private final Map<MethodInfo, InterceptionInfo> interceptedMethods;

    private final Map<InterceptionType, InterceptionInfo> lifecycleInterceptors;

    private final Integer alternativePriority;

    private final List<StereotypeInfo> stereotypes;

    private final String name;

    private final boolean isDefaultBean;

    // Gizmo consumers are only used by synthetic beans

    private final Consumer<MethodCreator> creatorConsumer;

    private final Consumer<MethodCreator> destroyerConsumer;

    private final Map<String, Object> params;

    BeanInfo(AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope, Set<Type> types,
            Set<AnnotationInstance> qualifiers,
            List<Injection> injections, BeanInfo declaringBean, DisposerInfo disposer, Integer alternativePriority,
            List<StereotypeInfo> stereotypes,
            String name, boolean isDefaultBean) {
        this(null, null, target, beanDeployment, scope, types, qualifiers, injections, declaringBean, disposer,
                alternativePriority,
                stereotypes, name, isDefaultBean, null, null,
                Collections.emptyMap());
    }

    BeanInfo(ClassInfo implClazz, Type providerType, AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope,
            Set<Type> types,
            Set<AnnotationInstance> qualifiers,
            List<Injection> injections, BeanInfo declaringBean, DisposerInfo disposer, Integer alternativePriority,
            List<StereotypeInfo> stereotypes,
            String name, boolean isDefaultBean, Consumer<MethodCreator> creatorConsumer,
            Consumer<MethodCreator> destroyerConsumer,
            Map<String, Object> params) {
        this.target = Optional.ofNullable(target);
        if (implClazz == null && target != null) {
            implClazz = initImplClazz(target, beanDeployment);
        }
        this.implClazz = implClazz;
        if (providerType == null) {
            providerType = initProviderType(target, implClazz);
        }
        this.providerType = providerType;
        this.beanDeployment = beanDeployment;
        this.scope = scope != null ? scope : BuiltinScope.DEPENDENT.getInfo();
        this.types = types;
        for (Type type : types) {
            Beans.analyzeType(type, beanDeployment);
        }
        if (qualifiers.isEmpty()
                || (qualifiers.size() <= 2 && qualifiers.stream()
                        .allMatch(a -> DotNames.NAMED.equals(a.name()) || DotNames.ANY.equals(a.name())))) {
            qualifiers.add(BuiltinQualifier.DEFAULT.getInstance());
        }
        qualifiers.add(BuiltinQualifier.ANY.getInstance());
        this.qualifiers = qualifiers;
        this.injections = injections;
        this.declaringBean = declaringBean;
        this.disposer = disposer;
        this.alternativePriority = alternativePriority;
        this.stereotypes = stereotypes;
        this.name = name;
        this.isDefaultBean = isDefaultBean;
        this.creatorConsumer = creatorConsumer;
        this.destroyerConsumer = destroyerConsumer;
        this.params = params;
        // Identifier must be unique for a specific deployment
        this.identifier = Hashes.sha1(toString());
        this.interceptedMethods = new ConcurrentHashMap<>();
        this.lifecycleInterceptors = new ConcurrentHashMap<>();
    }

    @Override
    public TargetKind kind() {
        return TargetKind.BEAN;
    }

    @Override
    public BeanInfo asBean() {
        return this;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Optional<AnnotationTarget> getTarget() {
        return target;
    }

    /**
     *
     * @return the impl class or null in case of a producer of a primitive type or an array
     */
    public ClassInfo getImplClazz() {
        return implClazz;
    }

    public boolean isClassBean() {
        return target.isPresent() && Kind.CLASS.equals(target.get().kind());
    }

    public boolean isProducerMethod() {
        return target.isPresent() && Kind.METHOD.equals(target.get().kind());
    }

    public boolean isProducerField() {
        return target.isPresent() && Kind.FIELD.equals(target.get().kind());
    }

    public boolean isSynthetic() {
        return !target.isPresent();
    }

    public DotName getBeanClass() {
        if (declaringBean != null) {
            return declaringBean.implClazz.name();
        }
        return implClazz.name();
    }

    public boolean isInterceptor() {
        return false;
    }

    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    BeanDeployment getDeployment() {
        return beanDeployment;
    }

    Type getProviderType() {
        return providerType;
    }

    public ScopeInfo getScope() {
        return scope;
    }

    public Set<Type> getTypes() {
        return types;
    }

    public boolean hasType(DotName typeName) {
        for (Type type : types) {
            if (type.name().equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    public Set<AnnotationInstance> getQualifiers() {
        return qualifiers;
    }

    public boolean hasDefaultQualifiers() {
        return qualifiers.size() == 2 && qualifiers.contains(BuiltinQualifier.DEFAULT.getInstance())
                && qualifiers.contains(BuiltinQualifier.ANY.getInstance());
    }

    List<Injection> getInjections() {
        return injections;
    }

    public List<InjectionPointInfo> getAllInjectionPoints() {
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
        return injections.isEmpty() ? Optional.empty() : injections.stream().filter(Injection::isConstructor).findAny();
    }

    Map<MethodInfo, InterceptionInfo> getInterceptedMethods() {
        return interceptedMethods;
    }

    InterceptionInfo getLifecycleInterceptors(InterceptionType interceptionType) {
        return lifecycleInterceptors.containsKey(interceptionType) ? lifecycleInterceptors.get(interceptionType)
                : InterceptionInfo.EMPTY;
    }

    public boolean hasLifecycleInterceptors() {
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
        if (lifecycleInterceptors.isEmpty() && interceptedMethods.isEmpty()) {
            return Collections.emptyList();
        }
        List<InterceptorInfo> bound = new ArrayList<>();
        for (InterceptionInfo interception : lifecycleInterceptors.values()) {
            for (InterceptorInfo interceptor : interception.interceptors) {
                if (!bound.contains(interceptor)) {
                    bound.add(interceptor);
                }
            }
        }
        for (InterceptionInfo interception : interceptedMethods.values()) {
            for (InterceptorInfo interceptor : interception.interceptors) {
                if (!bound.contains(interceptor)) {
                    bound.add(interceptor);
                }
            }
        }
        Collections.sort(bound);
        return bound;
    }

    public DisposerInfo getDisposer() {
        return disposer;
    }

    public boolean isAlternative() {
        return alternativePriority != null;
    }

    public Integer getAlternativePriority() {
        return alternativePriority;
    }

    public List<StereotypeInfo> getStereotypes() {
        return stereotypes;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaultBean() {
        return isDefaultBean;
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

    void validate(List<Throwable> errors, List<BeanDeploymentValidator> validators) {
        if (isClassBean()) {
            ClassInfo beanClass = target.get().asClass();
            String classifier = scope.isNormal() ? "Normal scoped" : null;
            if (classifier == null && isSubclassRequired()) {
                classifier = "Intercepted";
            }
            if (Modifier.isFinal(beanClass.flags()) && classifier != null) {
                errors.add(new DefinitionException(String.format("%s bean must not be final: %s", classifier, this)));
            }

            MethodInfo noArgsConstructor = beanClass.method(Methods.INIT);
            if (!ValidationRule.NO_ARGS_CONSTRUCTOR.skipFor(validators, this)) {
                // Note that spec also requires no-arg constructor for intercepted beans but intercepted subclasses should work fine with non-private @Inject
                // constructors so we only validate normal scoped beans
                if (scope.isNormal() && noArgsConstructor == null) {
                    errors.add(new DefinitionException(String
                            .format("Normal scoped beans must declare a non-private constructor with no parameters: %s",
                                    this)));
                }
            }
            if (noArgsConstructor != null && Modifier.isPrivate(noArgsConstructor.flags()) && classifier != null) {
                errors.add(
                        new DefinitionException(
                                String.format(
                                        "%s bean is not proxyable because it has a private no-args constructor: %s. To fix this problem, change the constructor to be package-private",
                                        classifier, this)));
            }

        } else if (isProducerField() || isProducerMethod()) {
            ClassInfo returnTypeClass = getClassByName(beanDeployment.getIndex(),
                    (isProducerMethod() ? target.get().asMethod().returnType() : target.get().asField().type()).name());
            // can be null for primitive types
            if (returnTypeClass != null && scope.isNormal() && !Modifier.isInterface(returnTypeClass.flags())) {
                String methodOrField = isProducerMethod() ? "method" : "field";
                String classifier = "Producer " + methodOrField + " for a normal scoped bean";
                if (Modifier.isFinal(returnTypeClass.flags())) {
                    errors.add(
                            new DefinitionException(String.format("%s must not have a" +
                                    " return type that is final: %s", classifier, this)));
                }
                MethodInfo noArgsConstructor = returnTypeClass.method(Methods.INIT);
                if (!ValidationRule.NO_ARGS_CONSTRUCTOR.skipFor(validators, this)) {
                    if (noArgsConstructor == null) {
                        errors.add(new DefinitionException(String
                                .format("Return type of a producer " + methodOrField + " for normal scoped beans must" +
                                        " declare a non-private constructor with no parameters: %s", this)));
                    }
                }
                if (noArgsConstructor != null && Modifier.isPrivate(noArgsConstructor.flags())) {
                    errors.add(
                            new DefinitionException(
                                    String.format(
                                            "%s is not proxyable because it has a private no-args constructor: %s.",
                                            classifier, this)));
                }
            }
        }
    }

    void init(List<Throwable> errors, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean removeFinalForProxyableMethods) {
        for (Injection injection : injections) {
            for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                Beans.resolveInjectionPoint(beanDeployment, this, injectionPoint, errors);
            }
        }
        if (disposer != null) {
            disposer.init(errors);
        }
        interceptedMethods.putAll(initInterceptedMethods(errors, bytecodeTransformerConsumer, removeFinalForProxyableMethods));
        if (errors.isEmpty()) {
            lifecycleInterceptors.putAll(initLifecycleInterceptors());
        }
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

    private Map<MethodInfo, InterceptionInfo> initInterceptedMethods(List<Throwable> errors,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean removeFinalForProxyableMethods) {
        if (!isInterceptor() && isClassBean()) {
            Map<MethodInfo, InterceptionInfo> interceptedMethods = new HashMap<>();
            Map<MethodKey, Set<AnnotationInstance>> candidates = new HashMap<>();

            List<AnnotationInstance> classLevelBindings = new ArrayList<>();
            addClassLevelBindings(target.get().asClass(), classLevelBindings);
            if (!stereotypes.isEmpty()) {
                for (StereotypeInfo stereotype : stereotypes) {
                    addClassLevelBindings(stereotype.getTarget(), classLevelBindings);
                }
            }

            Set<MethodInfo> finalMethods = Methods.addInterceptedMethodCandidates(beanDeployment, target.get().asClass(),
                    candidates, classLevelBindings, bytecodeTransformerConsumer, removeFinalForProxyableMethods);
            if (!finalMethods.isEmpty()) {
                errors.add(new DeploymentException(String.format(
                        "Intercepted methods of the bean %s may not be declared final:\n\t- %s", getBeanClass(),
                        finalMethods.stream().map(Object::toString).sorted().collect(Collectors.joining("\n\t- ")))));
                return Collections.emptyMap();
            }

            for (Entry<MethodKey, Set<AnnotationInstance>> entry : candidates.entrySet()) {
                List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver()
                        .resolve(InterceptionType.AROUND_INVOKE, entry.getValue());
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
            Set<AnnotationInstance> constructorLevelBindings = new HashSet<>();
            addClassLevelBindings(target.get().asClass(), classLevelBindings);
            addConstructorLevelBindings(target.get().asClass(), constructorLevelBindings);
            putLifecycleInterceptors(lifecycleInterceptors, classLevelBindings, InterceptionType.POST_CONSTRUCT);
            putLifecycleInterceptors(lifecycleInterceptors, classLevelBindings, InterceptionType.PRE_DESTROY);
            constructorLevelBindings.addAll(classLevelBindings);
            putLifecycleInterceptors(lifecycleInterceptors, constructorLevelBindings, InterceptionType.AROUND_CONSTRUCT);
            return lifecycleInterceptors;
        } else {
            return Collections.emptyMap();
        }
    }

    private void putLifecycleInterceptors(Map<InterceptionType, InterceptionInfo> lifecycleInterceptors,
            Set<AnnotationInstance> classLevelBindings,
            InterceptionType interceptionType) {
        List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver().resolve(interceptionType,
                classLevelBindings);
        if (!interceptors.isEmpty()) {
            lifecycleInterceptors.put(interceptionType, new InterceptionInfo(interceptors, classLevelBindings));
        }
    }

    private void addClassLevelBindings(ClassInfo classInfo, Collection<AnnotationInstance> bindings) {
        beanDeployment.getAnnotations(classInfo).stream()
                .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null
                        && bindings.stream().noneMatch(e -> e.name().equals(a.name())))
                .forEach(a -> bindings.add(a));
        if (classInfo.superClassType() != null && !classInfo.superClassType().name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = getClassByName(beanDeployment.getIndex(), classInfo.superName());
            if (superClass != null) {
                addClassLevelBindings(superClass, bindings);
            }
        }
    }

    private void addConstructorLevelBindings(ClassInfo classInfo, Collection<AnnotationInstance> bindings) {
        MethodInfo constructor;
        Optional<Injection> constructorWithInject = getConstructorInjection();
        if (constructorWithInject.isPresent()) {
            constructor = constructorWithInject.get().target.asMethod();
        } else {
            constructor = classInfo.method(Methods.INIT);
        }
        if (constructor != null) {
            beanDeployment.getAnnotations(constructor).stream()
                    .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null
                            && bindings.stream().noneMatch(e -> e.name().equals(a.name())))
                    .forEach(a -> bindings.add(a));
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
        builder.append(target.isPresent() ? target.get() : "n/a");
        if (declaringBean != null) {
            builder.append(", declaringBean=");
            builder.append(declaringBean.target.isPresent() ? declaringBean.target.get() : "n/a");
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BeanInfo other = (BeanInfo) obj;
        return Objects.equals(identifier, other.identifier);
    }

    private Type initProviderType(AnnotationTarget target, ClassInfo implClazz) {
        if (target != null) {
            switch (target.kind()) {
                case CLASS:
                    return Types.getProviderType(target.asClass());
                case FIELD:
                    return target.asField().type();
                case METHOD:
                    return target.asMethod().returnType();
                default:
                    break;
            }
        } else if (implClazz != null) {
            return Type.create(implClazz.name(), org.jboss.jandex.Type.Kind.CLASS);
        }
        throw new IllegalStateException("Cannot infer the provider type");
    }

    private ClassInfo initImplClazz(AnnotationTarget target, BeanDeployment beanDeployment) {
        switch (target.kind()) {
            case CLASS:
                return target.asClass();
            case FIELD:
                Type fieldType = target.asField().type();
                if (fieldType.kind() != org.jboss.jandex.Type.Kind.PRIMITIVE
                        && fieldType.kind() != org.jboss.jandex.Type.Kind.ARRAY) {
                    return getClassByName(beanDeployment.getIndex(), fieldType.name());
                }
                break;
            case METHOD:
                Type returnType = target.asMethod().returnType();
                if (returnType.kind() != org.jboss.jandex.Type.Kind.PRIMITIVE
                        && returnType.kind() != org.jboss.jandex.Type.Kind.ARRAY) {
                    return getClassByName(beanDeployment.getIndex(), returnType.name());
                }
                break;
            default:
                break;
        }
        return null;
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

    static class Builder {

        private ClassInfo implClazz;

        private Type providerType;

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

        private String name;

        private boolean isDefaultBean;

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

        Builder providerType(Type providerType) {
            this.providerType = providerType;
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

        Builder name(String name) {
            this.name = name;
            return this;
        }

        Builder defaultBean(boolean isDefaultBean) {
            this.isDefaultBean = isDefaultBean;
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
            return new BeanInfo(implClazz, providerType, target, beanDeployment, scope, types, qualifiers, injections,
                    declaringBean,
                    disposer, alternativePriority,
                    stereotypes, name, isDefaultBean, creatorConsumer, destroyerConsumer, params);
        }

    }

}
