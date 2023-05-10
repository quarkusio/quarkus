package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.InterceptionType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.Methods.MethodKey;
import io.quarkus.arc.processor.Methods.SubclassSkipPredicate;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

/**
 * Represents a CDI bean at build time.
 */
public class BeanInfo implements InjectionTargetInfo {

    private final String identifier;

    private final ClassInfo implClazz;

    private final Type providerType;

    protected final Optional<AnnotationTarget> target;

    private final BeanDeployment beanDeployment;

    protected final ScopeInfo scope;

    protected final Set<Type> types;

    protected final Set<AnnotationInstance> qualifiers;

    private final List<Injection> injections;

    private final BeanInfo declaringBean;

    private final DisposerInfo disposer;

    // These maps are initialized during BeanDeployment.init()
    private volatile Map<MethodInfo, InterceptionInfo> interceptedMethods;
    private volatile Map<MethodInfo, DecorationInfo> decoratedMethods;
    private volatile Map<InterceptionType, InterceptionInfo> lifecycleInterceptors;

    private final boolean alternative;
    private final Integer priority;

    private final List<StereotypeInfo> stereotypes;

    private final String name;

    private final boolean defaultBean;

    // Following fields are only used by synthetic beans

    private final boolean removable;

    private final Consumer<MethodCreator> creatorConsumer;

    private final Consumer<MethodCreator> destroyerConsumer;

    private final Map<String, Object> params;

    private final boolean forceApplicationClass;

    private final String targetPackageName;

    private final List<MethodInfo> aroundInvokes;

    BeanInfo(AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope, Set<Type> types,
            Set<AnnotationInstance> qualifiers, List<Injection> injections, BeanInfo declaringBean, DisposerInfo disposer,
            boolean alternative, List<StereotypeInfo> stereotypes, String name, boolean isDefaultBean, String targetPackageName,
            Integer priority) {
        this(null, null, target, beanDeployment, scope, types, qualifiers, injections, declaringBean, disposer,
                alternative, stereotypes, name, isDefaultBean, null, null, Collections.emptyMap(), true, false,
                targetPackageName, priority);
    }

    BeanInfo(ClassInfo implClazz, Type providerType, AnnotationTarget target, BeanDeployment beanDeployment, ScopeInfo scope,
            Set<Type> types, Set<AnnotationInstance> qualifiers, List<Injection> injections, BeanInfo declaringBean,
            DisposerInfo disposer, boolean alternative,
            List<StereotypeInfo> stereotypes, String name, boolean isDefaultBean, Consumer<MethodCreator> creatorConsumer,
            Consumer<MethodCreator> destroyerConsumer, Map<String, Object> params, boolean isRemovable,
            boolean forceApplicationClass, String targetPackageName, Integer priority) {

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
        types.add(ClassType.OBJECT_TYPE);
        this.types = types;
        for (Type type : types) {
            Beans.analyzeType(type, beanDeployment);
        }
        Beans.addImplicitQualifiers(qualifiers);
        this.qualifiers = qualifiers;
        this.injections = injections;
        this.declaringBean = declaringBean;
        this.disposer = disposer;
        this.alternative = alternative;
        this.priority = priority;
        this.stereotypes = stereotypes;
        this.name = name;
        this.defaultBean = isDefaultBean;
        this.creatorConsumer = creatorConsumer;
        this.destroyerConsumer = destroyerConsumer;
        this.removable = isRemovable;
        this.params = params;
        // Identifier must be unique for a specific deployment
        this.identifier = Hashes.sha1(toString() + beanDeployment.toString());
        this.interceptedMethods = Collections.emptyMap();
        this.decoratedMethods = Collections.emptyMap();
        this.lifecycleInterceptors = Collections.emptyMap();
        this.forceApplicationClass = forceApplicationClass;
        this.targetPackageName = targetPackageName;
        this.aroundInvokes = isInterceptor() || isDecorator() ? List.of() : Beans.getAroundInvokes(implClazz, beanDeployment);
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

    /**
     *
     * @return the annotation target or an empty optional in case of synthetic beans
     */
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

    public boolean isProducer() {
        return isProducerMethod() || isProducerField();
    }

    public boolean isStaticProducer() {
        if (isProducerField()) {
            return Modifier.isStatic(target.get().asField().flags());
        } else if (isProducerMethod()) {
            return Modifier.isStatic(target.get().asMethod().flags());
        } else {
            return false;
        }
    }

    public boolean isSynthetic() {
        return !target.isPresent();
    }

    public boolean isRemovable() {
        return removable;
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

    public boolean isDecorator() {
        return false;
    }

    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    BeanDeployment getDeployment() {
        return beanDeployment;
    }

    public Type getProviderType() {
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

    public Optional<AnnotationInstance> getQualifier(DotName dotName) {
        for (AnnotationInstance qualifier : qualifiers) {
            if (qualifier.name().equals(dotName)) {
                return Optional.of(qualifier);
            }
        }
        return Optional.empty();
    }

    public boolean hasDefaultQualifiers() {
        return qualifiers.size() == 2 && qualifiers.contains(BuiltinQualifier.DEFAULT.getInstance())
                && qualifiers.contains(BuiltinQualifier.ANY.getInstance());
    }

    List<Injection> getInjections() {
        return injections;
    }

    public boolean hasInjectionPoint() {
        return !injections.isEmpty();
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

    boolean requiresInjectionPointMetadata() {
        for (InjectionPointInfo injectionPoint : getAllInjectionPoints()) {
            if (DotNames.INJECTION_POINT.equals(injectionPoint.getRequiredType().name())) {
                return true;
            }
        }
        return false;
    }

    Optional<Injection> getConstructorInjection() {
        return injections.isEmpty() ? Optional.empty() : injections.stream().filter(Injection::isConstructor).findAny();
    }

    Map<MethodInfo, InterceptionInfo> getInterceptedMethods() {
        return interceptedMethods;
    }

    Map<MethodInfo, DecorationInfo> getDecoratedMethods() {
        return decoratedMethods;
    }

    /**
     * @return {@code true} if the bean has an associated interceptor with the given binding, {@code false} otherwise
     */
    public boolean hasAroundInvokeInterceptorWithBinding(DotName binding) {
        if (interceptedMethods.isEmpty()) {
            return false;
        }
        for (InterceptionInfo interception : interceptedMethods.values()) {
            if (Annotations.contains(interception.bindings, binding)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return an immutable map of intercepted methods to the set of interceptor bindings
     */
    public Map<MethodInfo, Set<AnnotationInstance>> getInterceptedMethodsBindings() {
        return interceptedMethods.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Entry::getKey, e -> Collections.unmodifiableSet(e.getValue().bindings)));
    }

    List<MethodInfo> getInterceptedOrDecoratedMethods() {
        Set<MethodInfo> methods = new HashSet<>(interceptedMethods.keySet());
        methods.addAll(decoratedMethods.keySet());
        List<MethodInfo> sorted = new ArrayList<>(methods);
        Collections.sort(sorted, Comparator.comparing(MethodInfo::toString));
        return sorted;
    }

    Set<MethodInfo> getDecoratedMethods(DecoratorInfo decorator) {
        Set<MethodInfo> decorated = new HashSet<>();
        for (Entry<MethodInfo, DecorationInfo> entry : decoratedMethods.entrySet()) {
            if (entry.getValue().decorators.contains(decorator)) {
                decorated.add(entry.getKey());
            }
        }
        return decorated;
    }

    // Returns a map of method descriptor -> next decorator in the chain
    // e.g. foo() -> BravoDecorator
    Map<MethodDescriptor, DecoratorInfo> getNextDecorators(DecoratorInfo decorator) {
        Map<MethodDescriptor, DecoratorInfo> next = new HashMap<>();
        for (Entry<MethodInfo, DecorationInfo> entry : decoratedMethods.entrySet()) {
            List<DecoratorInfo> decorators = entry.getValue().decorators;
            int index = decorators.indexOf(decorator);
            if (index != -1) {
                if (index != (decorators.size() - 1)) {
                    next.put(MethodDescriptor.of(entry.getKey()), decorators.get(index + 1));
                }
            }
        }
        return next;
    }

    InterceptionInfo getLifecycleInterceptors(InterceptionType interceptionType) {
        return lifecycleInterceptors.containsKey(interceptionType) ? lifecycleInterceptors.get(interceptionType)
                : InterceptionInfo.EMPTY;
    }

    public boolean hasLifecycleInterceptors() {
        return !lifecycleInterceptors.isEmpty();
    }

    public boolean hasAroundInvokeInterceptors() {
        return !interceptedMethods.isEmpty();
    }

    boolean isSubclassRequired() {
        return !interceptedMethods.isEmpty()
                || !decoratedMethods.isEmpty()
                || lifecycleInterceptors.containsKey(InterceptionType.PRE_DESTROY)
                || !aroundInvokes.isEmpty();
    }

    /**
     *
     * @return {@code true} if the bean requires some customized destroy logic
     */
    public boolean hasDestroyLogic() {
        if (isInterceptor() || isDecorator()) {
            return false;
        }
        if (disposer != null || destroyerConsumer != null) {
            // producer with disposer or custom bean with destruction logic
            return true;
        }
        // test class bean with @PreDestroy interceptor or callback
        return isClassBean() && (!getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()
                || !Beans.getCallbacks(target.get().asClass(), DotNames.PRE_DESTROY, beanDeployment.getBeanArchiveIndex())
                        .isEmpty());
    }

    public boolean isForceApplicationClass() {
        return forceApplicationClass;
    }

    /**
     * Note that the interceptors are not available until the bean is fully initialized, i.e. they are available after
     * {@link BeanProcessor#initialize(Consumer, List)}.
     *
     * @return an ordered list of all interceptors associated with the bean
     */
    public List<InterceptorInfo> getBoundInterceptors() {
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

    /**
     * Note that the decorators are not available until the bean is fully initialized, i.e. they are available after
     * {@link BeanProcessor#initialize(Consumer, List)}.
     *
     * @return an ordered list of all decorators associated with the bean
     */
    public List<DecoratorInfo> getBoundDecorators() {
        if (decoratedMethods.isEmpty()) {
            return Collections.emptyList();
        }
        List<DecoratorInfo> bound = new ArrayList<>();
        for (DecorationInfo decoration : decoratedMethods.values()) {
            for (DecoratorInfo decorator : decoration.decorators) {
                if (!bound.contains(decorator)) {
                    bound.add(decorator);
                }
            }
        }
        // Sort by priority (highest goes first) and by bean class (reversed lexicographic-order)
        // Highest priority first because the decorators are instantiated in the reverse order,
        // i.e. when the subclass constructor is generated the delegate subclass of the first decorator
        // (lower priority) needs a reference to the next decorator in the chain (higher priority)
        // Note that this set must be always reversed compared to the result coming from the BeanInfo#getNextDecorators(DecoratorInfo)
        Collections.sort(bound,
                Comparator.comparing(DecoratorInfo::getPriority)
                        .thenComparing(DecoratorInfo::getBeanClass)
                        .reversed());
        return bound;
    }

    /**
     *
     * @return the list of around invoke interceptor methods declared in the hierarchy of a bean class
     */
    List<MethodInfo> getAroundInvokes() {
        return aroundInvokes;
    }

    boolean hasAroundInvokes() {
        return !aroundInvokes.isEmpty();
    }

    public DisposerInfo getDisposer() {
        return disposer;
    }

    public boolean isAlternative() {
        return alternative;
    }

    public Integer getAlternativePriority() {
        return alternative ? priority : null;
    }

    public Integer getPriority() {
        return priority;
    }

    public List<StereotypeInfo> getStereotypes() {
        return stereotypes;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaultBean() {
        return defaultBean;
    }

    /**
     * @param requiredType
     * @param requiredQualifiers
     * @return {@code true} if this bean is assignable to the required type and qualifiers
     */
    public boolean isAssignableTo(Type requiredType, AnnotationInstance... requiredQualifiers) {
        Set<AnnotationInstance> qualifiers;
        if (requiredQualifiers.length == 0) {
            qualifiers = Collections.emptySet();
        } else {
            qualifiers = new HashSet<>();
            Collections.addAll(qualifiers, requiredQualifiers);
        }
        return beanDeployment.getBeanResolver().matches(this, requiredType, qualifiers);
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

    public String getTargetPackageName() {
        if (targetPackageName != null) {
            return targetPackageName;
        }
        DotName providerTypeName;
        if (isProducer()) {
            providerTypeName = declaringBean.getProviderType().name();
        } else {
            if (providerType.kind() == org.jboss.jandex.Type.Kind.ARRAY
                    || providerType.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE) {
                providerTypeName = implClazz.name();
            } else {
                providerTypeName = providerType.name();
            }
        }
        String packageName = DotNames.packageName(providerTypeName);
        if (packageName.startsWith("java.")) {
            // It is not possible to place a class in a JDK package
            packageName = AbstractGenerator.DEFAULT_PACKAGE;
        }
        return packageName;
    }

    public String getClientProxyPackageName() {
        if (isProducer()) {
            AnnotationTarget target = getTarget().get();
            DotName typeName = target.kind() == Kind.FIELD ? target.asField().type().name()
                    : target.asMethod().returnType().name();
            String packageName = DotNames.packageName(typeName);
            if (packageName.startsWith("java.")) {
                // It is not possible to place a class in a JDK package
                packageName = AbstractGenerator.DEFAULT_PACKAGE;
            }
            return packageName;
        } else {
            return getTargetPackageName();
        }
    }

    void validate(List<Throwable> errors, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            Set<DotName> classesReceivingNoArgsCtor) {
        Beans.validateBean(this, errors, bytecodeTransformerConsumer, classesReceivingNoArgsCtor);
    }

    void validateInterceptorDecorator(List<Throwable> errors, Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        // no actual validations done at the moment, but we still want the transformation
        Beans.validateInterceptorDecorator(this, errors, bytecodeTransformerConsumer);
    }

    void init(List<Throwable> errors, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        for (Injection injection : injections) {
            for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                if (injectionPoint.isDelegate() && !isDecorator()) {
                    errors.add(new DeploymentException(String.format(
                            "Only decorators can declare a delegate injection point: %s", this)));
                } else if (injectionPoint.getType().kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
                    errors.add(new DefinitionException(String.format("Type variable is not a legal injection point type: %s",
                            injectionPoint.getTargetInfo())));
                } else {
                    Beans.resolveInjectionPoint(beanDeployment, this, injectionPoint, errors);
                }
            }
        }
        if (disposer != null) {
            disposer.init(errors);
        }
        interceptedMethods = Map
                .copyOf(initInterceptedMethods(errors, bytecodeTransformerConsumer, transformUnproxyableClasses));
        decoratedMethods = Map.copyOf(initDecoratedMethods());
        if (errors.isEmpty()) {
            lifecycleInterceptors = Map.copyOf(initLifecycleInterceptors());
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
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {
        if (!isInterceptor() && !isDecorator() && isClassBean()) {
            Map<MethodInfo, InterceptionInfo> interceptedMethods = new HashMap<>();
            Map<MethodKey, Set<AnnotationInstance>> candidates = new HashMap<>();

            ClassInfo targetClass = target.get().asClass();
            List<AnnotationInstance> classLevelBindings = new ArrayList<>();
            addClassLevelBindings(targetClass, classLevelBindings);
            Interceptors.checkClassLevelInterceptorBindings(classLevelBindings, targetClass, beanDeployment);

            Set<MethodInfo> finalMethods = Methods.addInterceptedMethodCandidates(this, candidates, classLevelBindings,
                    bytecodeTransformerConsumer, transformUnproxyableClasses);
            if (!finalMethods.isEmpty()) {
                String additionalError = "";
                if (finalMethods.stream().anyMatch(KotlinUtils::isNoninterceptableKotlinMethod)) {
                    additionalError = "\n\tKotlin `suspend` functions must be `open` and declared in `open` classes, "
                            + "otherwise they cannot be intercepted";
                }
                errors.add(new DeploymentException(String.format(
                        "Intercepted methods of the bean %s may not be declared final:\n\t- %s%s", getBeanClass(),
                        finalMethods.stream().map(Object::toString).sorted().collect(Collectors.joining("\n\t- ")),
                        additionalError)));
                return Collections.emptyMap();
            }

            for (Entry<MethodKey, Set<AnnotationInstance>> entry : candidates.entrySet()) {
                List<InterceptorInfo> interceptors = beanDeployment.getInterceptorResolver()
                        .resolve(InterceptionType.AROUND_INVOKE, entry.getValue());
                if (!interceptors.isEmpty() || !aroundInvokes.isEmpty()) {
                    interceptedMethods.put(entry.getKey().method, new InterceptionInfo(interceptors, entry.getValue()));
                }
            }
            return interceptedMethods;
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<MethodInfo, DecorationInfo> initDecoratedMethods() {
        Collection<DecoratorInfo> decorators = beanDeployment.getDecorators();
        if (decorators.isEmpty() || isInterceptor() || isDecorator() || !isClassBean()) {
            return Collections.emptyMap();
        }
        // A decorator is bound to a bean if the bean is assignable to the delegate injection point
        List<DecoratorInfo> bound = new ArrayList<>();
        for (DecoratorInfo decorator : decorators) {
            // make sure we use delegate injection point assignability rules in this case
            if (beanDeployment.delegateInjectionPointResolver.matches(this,
                    decorator.getDelegateInjectionPoint().getTypeAndQualifiers())) {
                bound.add(decorator);
            }
        }
        // Decorators with the smaller priority values are called first
        Collections.sort(bound, Comparator.comparingInt(DecoratorInfo::getPriority).thenComparing(DecoratorInfo::getBeanClass));

        Map<MethodKey, DecorationInfo> candidates = new HashMap<>();
        ClassInfo classInfo = target.get().asClass();
        addDecoratedMethods(candidates, classInfo, classInfo, bound,
                new SubclassSkipPredicate(beanDeployment.getAssignabilityCheck()::isAssignableFrom,
                        beanDeployment.getBeanArchiveIndex(), beanDeployment.getObserverAndProducerMethods(),
                        beanDeployment.getAnnotationStore()));

        Map<MethodInfo, DecorationInfo> decoratedMethods = new HashMap<>(candidates.size());
        for (Entry<MethodKey, DecorationInfo> entry : candidates.entrySet()) {
            decoratedMethods.put(entry.getKey().method, entry.getValue());
        }
        return decoratedMethods;
    }

    private void addDecoratedMethods(Map<MethodKey, DecorationInfo> decoratedMethods, ClassInfo classInfo,
            ClassInfo originalClassInfo, List<DecoratorInfo> boundDecorators, SubclassSkipPredicate skipPredicate) {
        skipPredicate.startProcessing(classInfo, originalClassInfo);
        for (MethodInfo method : classInfo.methods()) {
            if (skipPredicate.test(method)) {
                continue;
            }
            List<DecoratorInfo> matching = findMatchingDecorators(method, boundDecorators);
            if (!matching.isEmpty()) {
                decoratedMethods.computeIfAbsent(new MethodKey(method), key -> new DecorationInfo(matching));
            }
        }
        skipPredicate.methodsProcessed();
        if (!classInfo.superName().equals(DotNames.OBJECT)) {
            ClassInfo superClassInfo = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClassInfo != null) {
                addDecoratedMethods(decoratedMethods, superClassInfo, originalClassInfo, boundDecorators, skipPredicate);
            }
        }
    }

    private List<DecoratorInfo> findMatchingDecorators(MethodInfo method, List<DecoratorInfo> decorators) {
        List<Type> methodParams = method.parameterTypes();
        List<DecoratorInfo> matching = new ArrayList<>(decorators.size());
        for (DecoratorInfo decorator : decorators) {
            for (Type decoratedType : decorator.getDecoratedTypes()) {
                // Converter<String>
                ClassInfo decoratedTypeClass = decorator.getDeployment().getBeanArchiveIndex()
                        .getClassByName(decoratedType.name());
                if (decoratedTypeClass == null) {
                    throw new DefinitionException(
                            "The class of the decorated type " + decoratedType + " was not found in the index");
                }

                Map<String, Type> resolvedTypeParameters = Types.resolveDecoratedTypeParams(decoratedTypeClass,
                        decorator);

                for (MethodInfo decoratedMethod : decoratedTypeClass.methods()) {
                    if (!method.name().equals(decoratedMethod.name())) {
                        continue;
                    }
                    List<Type> decoratedMethodParams = decoratedMethod.parameterTypes();
                    if (methodParams.size() != decoratedMethodParams.size()) {
                        continue;
                    }
                    // Match the resolved parameter types
                    boolean matches = true;
                    decoratedMethodParams = Types.getResolvedParameters(decoratedTypeClass, resolvedTypeParameters,
                            decoratedMethod,
                            beanDeployment.getBeanArchiveIndex());
                    for (int i = 0; i < methodParams.size(); i++) {
                        if (!beanDeployment.getDelegateInjectionPointResolver().matches(decoratedMethodParams.get(i),
                                methodParams.get(i))) {
                            matches = false;
                        }
                    }
                    if (matches) {
                        matching.add(decorator);
                    }
                }
            }
        }
        return matching;
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
            MethodInfo interceptedConstructor = findInterceptedConstructor(target.get().asClass());
            if (beanDeployment.getAnnotation(interceptedConstructor, DotNames.NO_CLASS_INTERCEPTORS) == null) {
                constructorLevelBindings = Methods.mergeMethodAndClassLevelBindings(constructorLevelBindings,
                        classLevelBindings);
            }
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

    private void addClassLevelBindings(ClassInfo targetClass, Collection<AnnotationInstance> bindings) {
        List<AnnotationInstance> classLevelBindings = new ArrayList<>();
        doAddClassLevelBindings(targetClass, classLevelBindings, Set.of());
        bindings.addAll(classLevelBindings);
        if (!stereotypes.isEmpty()) {
            // interceptor binding declared on a bean class replaces an interceptor binding of the same type
            // declared by a stereotype that is applied to the bean class
            Set<DotName> skip = new HashSet<>();
            for (AnnotationInstance classLevelBinding : classLevelBindings) {
                skip.add(classLevelBinding.name());
            }
            for (StereotypeInfo stereotype : Beans.stereotypesWithTransitive(stereotypes,
                    beanDeployment.getStereotypesMap())) {
                doAddClassLevelBindings(stereotype.getTarget(), bindings, skip);
            }
        }
    }

    // bindings whose class name is present in `skip` are ignored (this is used to ignore bindings on stereotypes
    // when the original class has a binding of the same type)
    private void doAddClassLevelBindings(ClassInfo classInfo, Collection<AnnotationInstance> bindings, Set<DotName> skip) {
        beanDeployment.getAnnotations(classInfo).stream()
                .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null)
                .filter(a -> !skip.contains(a.name()))
                .forEach(bindings::add);
        if (classInfo.superClassType() != null && !classInfo.superClassType().name().equals(DotNames.OBJECT)) {
            ClassInfo superClass = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClass != null) {
                doAddClassLevelBindings(superClass, bindings, skip);
            }
        }
    }

    private MethodInfo findInterceptedConstructor(ClassInfo clazz) {
        Optional<Injection> constructorWithInject = getConstructorInjection();
        if (constructorWithInject.isPresent()) {
            return constructorWithInject.get().target.asMethod();
        } else {
            return clazz.method(Methods.INIT);
        }
    }

    private void addConstructorLevelBindings(ClassInfo classInfo, Collection<AnnotationInstance> bindings) {
        MethodInfo constructor = findInterceptedConstructor(classInfo);
        if (constructor != null) {
            beanDeployment.getAnnotations(constructor).stream()
                    .flatMap(a -> beanDeployment.extractInterceptorBindings(a).stream())
                    .forEach(bindings::add);
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
        return identifier.hashCode();
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
                return getClassByName(beanDeployment.getBeanArchiveIndex(), target.asField().type());
            case METHOD:
                return getClassByName(beanDeployment.getBeanArchiveIndex(), target.asMethod().returnType());
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

    static class DecorationInfo {

        final List<DecoratorInfo> decorators;

        public DecorationInfo(List<DecoratorInfo> decorators) {
            this.decorators = decorators;
        }

        boolean isEmpty() {
            return decorators.isEmpty();
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

        private boolean alternative;

        private List<StereotypeInfo> stereotypes;

        private String name;

        private boolean isDefaultBean;

        private Consumer<MethodCreator> creatorConsumer;

        private Consumer<MethodCreator> destroyerConsumer;

        private Map<String, Object> params;

        private boolean removable = true;

        private boolean forceApplicationClass;

        private String targetPackageName;

        private Integer priority;

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

        /**
         * @deprecated use {@link #alternative(boolean)} and {@link #priority(Integer)};
         *             this method will be removed at some time after Quarkus 3.6
         */
        @Deprecated(forRemoval = true, since = "3.0")
        Builder alternativePriority(Integer alternativePriority) {
            return alternative(true).priority(alternativePriority);
        }

        Builder alternative(boolean value) {
            this.alternative = value;
            return this;
        }

        Builder priority(Integer value) {
            this.priority = value;
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

        Builder removable(boolean val) {
            this.removable = val;
            return this;
        }

        Builder targetPackageName(String name) {
            this.targetPackageName = name;
            return this;
        }

        BeanInfo build() {
            return new BeanInfo(implClazz, providerType, target, beanDeployment, scope, types, qualifiers, injections,
                    declaringBean, disposer, alternative, stereotypes, name, isDefaultBean, creatorConsumer,
                    destroyerConsumer, params, removable, forceApplicationClass, targetPackageName, priority);
        }

        public Builder forceApplicationClass(boolean forceApplicationClass) {
            this.forceApplicationClass = forceApplicationClass;
            return this;
        }
    }

}
