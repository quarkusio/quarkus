package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.BeanProcessor.initAndSort;
import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanProcessor.BuildContextImpl;
import io.quarkus.arc.processor.BeanRegistrar.RegistrationContext;
import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.arc.processor.bcextensions.ExtensionsEntryPoint;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

public class BeanDeployment {

    private static final Logger LOGGER = Logger.getLogger(BeanDeployment.class);

    private final String name;
    private final BuildContextImpl buildContext;

    private final IndexView beanArchiveComputingIndex;
    private final IndexView beanArchiveImmutableIndex;
    private final IndexView applicationIndex;

    private final Map<DotName, ClassInfo> qualifiers;
    private final Map<DotName, ClassInfo> repeatingQualifierAnnotations;
    private final Map<DotName, Set<String>> qualifierNonbindingMembers;

    private final Map<DotName, ClassInfo> interceptorBindings;
    private final Map<DotName, ClassInfo> repeatingInterceptorBindingAnnotations;
    private final Map<DotName, Set<String>> interceptorNonbindingMembers;
    private final Map<DotName, Set<AnnotationInstance>> transitiveInterceptorBindings;

    private final Map<DotName, StereotypeInfo> stereotypes;

    private final List<BeanInfo> beans;
    private volatile Map<DotName, List<BeanInfo>> beansByType;

    private final List<InterceptorInfo> interceptors;
    private final List<DecoratorInfo> decorators;

    private final List<ObserverInfo> observers;

    final BeanResolverImpl beanResolver;
    final DelegateInjectionPointResolverImpl delegateInjectionPointResolver;
    private final AssignabilityCheck assignabilityCheck;

    private final InterceptorResolver interceptorResolver;

    private final AnnotationStore annotationStore;

    private final InjectionPointModifier injectionPointTransformer;

    private final List<ObserverTransformer> observerTransformers;

    private final Set<DotName> resourceAnnotations;

    private final List<InjectionPointInfo> injectionPoints;

    private final boolean removeUnusedBeans;

    private final List<Predicate<BeanInfo>> unusedExclusions;

    private final Set<BeanInfo> removedBeans;

    private final Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts;

    private final Map<DotName, BeanDefiningAnnotation> beanDefiningAnnotations;

    final boolean transformUnproxyableClasses;

    final boolean transformPrivateInjectedFields;

    final boolean failOnInterceptedPrivateMethod;

    private final boolean jtaCapabilities;

    final boolean strictCompatibility;

    private final AlternativePriorities alternativePriorities;

    private final List<Predicate<ClassInfo>> excludeTypes;

    private final ExtensionsEntryPoint buildCompatibleExtensions;

    BeanDeployment(String name, BuildContextImpl buildContext, BeanProcessor.Builder builder) {
        this.name = name;
        this.buildCompatibleExtensions = builder.buildCompatibleExtensions;
        this.buildContext = buildContext;
        Map<DotName, BeanDefiningAnnotation> beanDefiningAnnotations = new HashMap<>();
        if (builder.additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation bda : builder.additionalBeanDefiningAnnotations) {
                beanDefiningAnnotations.put(bda.getAnnotation(), bda);
            }
        }
        this.beanDefiningAnnotations = beanDefiningAnnotations;
        this.resourceAnnotations = new HashSet<>(builder.resourceAnnotations);
        this.beanArchiveComputingIndex = builder.beanArchiveComputingIndex;
        this.beanArchiveImmutableIndex = Objects.requireNonNull(builder.beanArchiveImmutableIndex);
        this.applicationIndex = builder.applicationIndex;
        this.annotationStore = new AnnotationStore(initAndSort(builder.annotationTransformers, buildContext), buildContext);
        if (buildContext != null) {
            buildContext.putInternal(Key.ANNOTATION_STORE.asString(), annotationStore);
        }
        this.injectionPointTransformer = new InjectionPointModifier(
                initAndSort(builder.injectionPointTransformers, buildContext), buildContext);
        this.observerTransformers = initAndSort(builder.observerTransformers, buildContext);
        this.removeUnusedBeans = builder.removeUnusedBeans;
        this.unusedExclusions = removeUnusedBeans ? new ArrayList<>(builder.removalExclusions) : null;
        this.removedBeans = removeUnusedBeans ? new CopyOnWriteArraySet<>() : Collections.emptySet();
        this.customContexts = new ConcurrentHashMap<>();

        this.excludeTypes = builder.excludeTypes != null ? new ArrayList<>(builder.excludeTypes) : Collections.emptyList();

        qualifierNonbindingMembers = new HashMap<>();
        qualifiers = findQualifiers();
        for (QualifierRegistrar registrar : builder.qualifierRegistrars) {
            for (Map.Entry<DotName, Set<String>> entry : registrar.getAdditionalQualifiers().entrySet()) {
                DotName dotName = entry.getKey();
                ClassInfo classInfo = getClassByName(getBeanArchiveIndex(), dotName);
                if (classInfo != null) {
                    Set<String> nonbindingMembers = entry.getValue();
                    if (nonbindingMembers == null) {
                        nonbindingMembers = Collections.emptySet();
                    }
                    qualifierNonbindingMembers.put(dotName, nonbindingMembers);
                    qualifiers.put(dotName, classInfo);
                }
            }
        }
        repeatingQualifierAnnotations = findContainerAnnotations(qualifiers);
        buildContextPut(Key.QUALIFIERS.asString(), Collections.unmodifiableMap(qualifiers));

        interceptorNonbindingMembers = new HashMap<>();
        interceptorBindings = findInterceptorBindings();
        for (InterceptorBindingRegistrar registrar : builder.interceptorBindingRegistrars) {
            for (InterceptorBindingRegistrar.InterceptorBinding binding : registrar.getAdditionalBindings()) {
                DotName dotName = binding.getName();
                ClassInfo annotationClass = getClassByName(getBeanArchiveIndex(), dotName);
                if (annotationClass != null) {
                    Set<String> nonbinding = new HashSet<>();
                    for (MethodInfo method : annotationClass.methods()) {
                        if (binding.isNonbinding(method.name())) {
                            nonbinding.add(method.name());
                        }
                    }
                    interceptorNonbindingMembers.put(dotName, nonbinding);
                }
                interceptorBindings.put(dotName, annotationClass);
            }
        }
        repeatingInterceptorBindingAnnotations = findContainerAnnotations(interceptorBindings);
        buildContextPut(Key.INTERCEPTOR_BINDINGS.asString(), Collections.unmodifiableMap(interceptorBindings));

        Set<DotName> additionalStereotypes = new HashSet<>();
        for (StereotypeRegistrar stereotypeRegistrar : builder.stereotypeRegistrars) {
            additionalStereotypes.addAll(stereotypeRegistrar.getAdditionalStereotypes());
        }

        this.stereotypes = findStereotypes(interceptorBindings, customContexts, additionalStereotypes,
                annotationStore);
        buildContextPut(Key.STEREOTYPES.asString(), Collections.unmodifiableMap(stereotypes));

        this.transitiveInterceptorBindings = findTransitiveInterceptorBindings(interceptorBindings.keySet(),
                new HashMap<>(), interceptorBindings, annotationStore);

        this.injectionPoints = new CopyOnWriteArrayList<>();
        this.interceptors = new CopyOnWriteArrayList<>();
        this.decorators = new CopyOnWriteArrayList<>();
        this.beans = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();

        this.assignabilityCheck = new AssignabilityCheck(getBeanArchiveIndex(), applicationIndex);
        this.beanResolver = new BeanResolverImpl(this);
        this.delegateInjectionPointResolver = new DelegateInjectionPointResolverImpl(this);
        this.interceptorResolver = new InterceptorResolver(this);
        this.transformUnproxyableClasses = builder.transformUnproxyableClasses;
        this.transformPrivateInjectedFields = builder.transformPrivateInjectedFields;
        this.failOnInterceptedPrivateMethod = builder.failOnInterceptedPrivateMethod;
        this.jtaCapabilities = builder.jtaCapabilities;
        this.strictCompatibility = builder.strictCompatibility;
        this.alternativePriorities = builder.alternativePriorities;
    }

    ContextRegistrar.RegistrationContext registerCustomContexts(List<ContextRegistrar> contextRegistrars) {
        io.quarkus.arc.processor.ContextRegistrar.RegistrationContext registrationContext = new io.quarkus.arc.processor.ContextRegistrar.RegistrationContext() {
            @Override
            public <V> V put(Key<V> key, V value) {
                return buildContext.put(key, value);
            }

            @Override
            public <V> V get(Key<V> key) {
                return buildContext.get(key);
            }

            @Override
            public ContextConfigurator configure(Class<? extends Annotation> scopeAnnotation) {
                return new ContextConfigurator(scopeAnnotation,
                        c -> {
                            ScopeInfo scope = new ScopeInfo(c.scopeAnnotation, c.isNormal);
                            beanDefiningAnnotations.put(scope.getDotName(),
                                    new BeanDefiningAnnotation(scope.getDotName(), null));
                            customContexts.put(scope, c.creator);
                        });
            }
        };
        for (ContextRegistrar contextRegistrar : contextRegistrars) {
            contextRegistrar.register(registrationContext);
        }
        return registrationContext;
    }

    void registerScopes() {
        if (buildContext != null) {
            List<ScopeInfo> allScopes = Arrays.stream(BuiltinScope.values()).map(i -> i.getInfo()).collect(Collectors.toList());
            allScopes.addAll(customContexts.keySet());
            buildContext.putInternal(Key.SCOPES.asString(), Collections.unmodifiableList(allScopes));
        }
    }

    BeanRegistrar.RegistrationContext registerBeans(List<BeanRegistrar> beanRegistrars) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        this.beans.addAll(
                findBeans(initBeanDefiningAnnotations(beanDefiningAnnotations.values(), stereotypes.keySet()), observers,
                        injectionPoints, jtaCapabilities));
        // Note that we use unmodifiable views because the underlying collections may change in the next phase
        // E.g. synthetic beans are added and unused interceptors removed
        buildContextPut(Key.BEANS.asString(), Collections.unmodifiableList(beans));
        buildContextPut(Key.OBSERVERS.asString(), Collections.unmodifiableList(observers));
        this.interceptors.addAll(findInterceptors(injectionPoints));
        buildContextPut(Key.INTERCEPTORS.asString(), Collections.unmodifiableList(interceptors));
        this.decorators.addAll(findDecorators(injectionPoints));
        buildContextPut(Key.DECORATORS.asString(), Collections.unmodifiableList(decorators));
        this.injectionPoints.addAll(injectionPoints);
        buildContextPut(Key.INJECTION_POINTS.asString(), Collections.unmodifiableList(this.injectionPoints));

        if (buildCompatibleExtensions != null) {
            buildCompatibleExtensions.runRegistration(beanArchiveComputingIndex, beans, observers);
        }

        return registerSyntheticBeans(beanRegistrars, buildContext);
    }

    void init(Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            List<Predicate<BeanInfo>> additionalUnusedBeanExclusions) {
        long start = System.nanoTime();

        // Collect dependency resolution errors
        List<Throwable> errors = new ArrayList<>();
        for (BeanInfo bean : beans) {
            bean.init(errors, bytecodeTransformerConsumer, transformUnproxyableClasses);
        }
        for (ObserverInfo observer : observers) {
            observer.init(errors);
        }
        for (InterceptorInfo interceptor : interceptors) {
            interceptor.init(errors, bytecodeTransformerConsumer, transformUnproxyableClasses);
        }
        for (DecoratorInfo decorator : decorators) {
            decorator.init(errors, bytecodeTransformerConsumer, transformUnproxyableClasses);
        }

        processErrors(errors);
        List<Predicate<BeanInfo>> allUnusedExclusions = new ArrayList<>(additionalUnusedBeanExclusions);
        if (unusedExclusions != null) {
            allUnusedExclusions.addAll(unusedExclusions);
        }

        if (removeUnusedBeans) {
            long removalStart = System.nanoTime();
            Set<BeanInfo> declaresObserver = observers.stream().map(ObserverInfo::getDeclaringBean).collect(Collectors.toSet());
            Set<DecoratorInfo> removedDecorators = new HashSet<>();
            Set<InterceptorInfo> removedInterceptors = new HashSet<>();
            removeUnusedComponents(declaresObserver, allUnusedExclusions, removedDecorators, removedInterceptors);

            LOGGER.debugf("Removed %s beans, %s interceptors and %s decorators in %s ms", removedBeans.size(),
                    removedInterceptors.size(), removedDecorators.size(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - removalStart));
            //we need to re-initialize it, so it does not contain removed beans
            initBeanByTypeMap();
            buildContext.putInternal(BuildExtension.Key.REMOVED_INTERCEPTORS.asString(),
                    Collections.unmodifiableSet(removedInterceptors));
            buildContext.putInternal(BuildExtension.Key.REMOVED_DECORATORS.asString(),
                    Collections.unmodifiableSet(removedDecorators));
        }
        buildContext.putInternal(BuildExtension.Key.REMOVED_BEANS.asString(), Collections.unmodifiableSet(removedBeans));
        LOGGER.debugf("Bean deployment initialized in %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    /**
     * Re-initialize the map that is used to speed-up lookup requests.
     */
    public void initBeanByTypeMap() {
        Map<DotName, List<BeanInfo>> map = new HashMap<>();
        for (BeanInfo bean : beans) {
            bean.types.stream().map(Type::name).distinct().forEach(rawTypeName -> {
                if (DotNames.OBJECT.equals(rawTypeName)) {
                    // Every bean has java.lang.Object - no need to cache results here
                    return;
                }
                List<BeanInfo> beans = map.get(rawTypeName);
                if (beans == null) {
                    // Very often, there will be exactly one bean for a given type
                    map.put(rawTypeName, List.of(bean));
                } else {
                    if (beans.size() == 1) {
                        map.put(rawTypeName, List.of(beans.get(0), bean));
                    } else {
                        BeanInfo[] array = new BeanInfo[beans.size() + 1];
                        for (int i = 0; i < beans.size(); i++) {
                            array[i] = beans.get(i);
                        }
                        array[beans.size()] = bean;
                        map.put(rawTypeName, List.of(array));
                    }
                }
            });
        }
        this.beansByType = map;
    }

    private void removeUnusedComponents(Set<BeanInfo> declaresObserver,
            List<Predicate<BeanInfo>> allUnusedExclusions, Set<DecoratorInfo> removedDecorators,
            Set<InterceptorInfo> removedInterceptors) {
        int removed;
        do {
            removed = 0;
            removed += removeUnusedBeans(declaresObserver, allUnusedExclusions).size();
            removed += removeUnusedInterceptors(removedInterceptors, allUnusedExclusions).size();
            removed += removeUnusedDecorators(removedDecorators, allUnusedExclusions).size();
        } while (removed > 0);
    }

    private Set<InterceptorInfo> removeUnusedInterceptors(Set<InterceptorInfo> removedInterceptors,
            List<Predicate<BeanInfo>> allUnusedExclusions) {
        Set<InterceptorInfo> removableInterceptors = new HashSet<>();
        for (InterceptorInfo interceptor : this.interceptors) {
            boolean removable = true;
            for (Predicate<BeanInfo> exclusion : allUnusedExclusions) {
                if (exclusion.test(interceptor)) {
                    removable = false;
                    break;
                }
            }
            if (removable) {
                for (BeanInfo bean : this.beans) {
                    if (bean.getBoundInterceptors().contains(interceptor)) {
                        removable = false;
                        break;
                    }
                }
            }
            if (removable) {
                removableInterceptors.add(interceptor);
            }
        }
        if (!removableInterceptors.isEmpty()) {
            removedInterceptors.addAll(removableInterceptors);
            this.interceptors.removeAll(removableInterceptors);
            List<InjectionPointInfo> removableInjectionPoints = new ArrayList<>();
            for (BeanInfo interceptor : removableInterceptors) {
                removableInjectionPoints.addAll(interceptor.getAllInjectionPoints());
            }
            injectionPoints.removeAll(removableInjectionPoints);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf(removableInterceptors.stream().map(i -> "Removed unused interceptor " + i)
                        .collect(Collectors.joining("\n")));
            }
        }
        return removableInterceptors;
    }

    private Set<DecoratorInfo> removeUnusedDecorators(Set<DecoratorInfo> removedDecorators,
            List<Predicate<BeanInfo>> allUnusedExclusions) {
        Set<DecoratorInfo> removableDecorators = new HashSet<>();
        for (DecoratorInfo decorator : this.decorators) {
            boolean removable = true;
            for (Predicate<BeanInfo> exclusion : allUnusedExclusions) {
                if (exclusion.test(decorator)) {
                    removable = false;
                    break;
                }
            }
            if (removable) {
                for (BeanInfo bean : this.beans) {
                    if (bean.getBoundDecorators().contains(decorator)) {
                        removable = false;
                        break;
                    }
                }
            }
            if (removable) {
                removableDecorators.add(decorator);
            }
        }
        if (!removableDecorators.isEmpty()) {
            removedDecorators.addAll(removableDecorators);
            this.decorators.removeAll(removableDecorators);
            List<InjectionPointInfo> removableInjectionPoints = new ArrayList<>();
            for (BeanInfo decorator : removableDecorators) {
                removableInjectionPoints.addAll(decorator.getAllInjectionPoints());
            }
            injectionPoints.removeAll(removableInjectionPoints);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf(removableDecorators.stream().map(i -> "Removed unused decorator " + i)
                        .collect(Collectors.joining("\n")));
            }
        }
        return removableDecorators;
    }

    private Set<BeanInfo> removeUnusedBeans(Set<BeanInfo> declaresObserver, List<Predicate<BeanInfo>> allUnusedExclusions) {
        Set<BeanInfo> removableBeans = UnusedBeans.findRemovableBeans(this.beans, this.injectionPoints, declaresObserver,
                allUnusedExclusions);
        if (!removableBeans.isEmpty()) {
            this.beans.removeAll(removableBeans);
            this.removedBeans.addAll(removableBeans);
            List<InjectionPointInfo> removableInjectionPoints = new ArrayList<>();
            for (BeanInfo bean : removableBeans) {
                removableInjectionPoints.addAll(bean.getAllInjectionPoints());
            }
            injectionPoints.removeAll(removableInjectionPoints);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf(removedBeans.stream().map(b -> "Removed unused " + b).collect(Collectors.joining("\n")));
            }
        }
        return removableBeans;
    }

    ValidationContext validate(List<BeanDeploymentValidator> validators,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        // Validate the bean deployment
        List<Throwable> errors = new ArrayList<>();
        // First, validate all beans internally
        validateBeans(errors, bytecodeTransformerConsumer);
        validateInterceptorsAndDecorators(errors, bytecodeTransformerConsumer);
        ValidationContextImpl validationContext = new ValidationContextImpl(buildContext);
        for (Throwable error : errors) {
            validationContext.addDeploymentProblem(error);
        }
        // Next, execute all registered validators
        for (BeanDeploymentValidator validator : validators) {
            validator.validate(validationContext);
        }
        return validationContext;
    }

    public Collection<BeanInfo> getBeans() {
        return Collections.unmodifiableList(beans);
    }

    Collection<BeanInfo> getBeansByRawType(DotName typeName) {
        var ret = beansByType.get(typeName);
        if (ret == null) {
            return Collections.emptyList();
        }
        return ret;
    }

    public Collection<BeanInfo> getRemovedBeans() {
        return Collections.unmodifiableSet(removedBeans);
    }

    public Collection<ClassInfo> getQualifiers() {
        return Collections.unmodifiableCollection(qualifiers.values());
    }

    Map<DotName, Set<String>> getQualifierNonbindingMembers() {
        return qualifierNonbindingMembers;
    }

    /**
     *
     * @return the collection of interceptor bindings; the container annotations of repeating interceptor binding are not
     *         included
     * @see #extractInterceptorBindings(AnnotationInstance)
     */
    public Collection<ClassInfo> getInterceptorBindings() {
        return Collections.unmodifiableCollection(interceptorBindings.values());
    }

    public Collection<InjectionPointInfo> getInjectionPoints() {
        return Collections.unmodifiableList(injectionPoints);
    }

    public Collection<ObserverInfo> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    public Collection<InterceptorInfo> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }

    public Collection<DecoratorInfo> getDecorators() {
        return Collections.unmodifiableList(decorators);
    }

    public Collection<StereotypeInfo> getStereotypes() {
        return Collections.unmodifiableCollection(stereotypes.values());
    }

    Map<DotName, StereotypeInfo> getStereotypesMap() {
        return Collections.unmodifiableMap(stereotypes);
    }

    /**
     * Returns the index that was used during discovery and type-safe resolution.
     * <p>
     * In general, the returned index is usually "computing" which means that it attempts to compute the information for the
     * classes that were not part of the initial bean archive index. I.e. the returned index corresponds to
     * {@link BeanProcessor.Builder#setComputingBeanArchiveIndex(IndexView)}. However, if the computing index was not set then
     * the index set by {@link BeanProcessor.Builder#setImmutableBeanArchiveIndex(IndexView)} is used instead.
     *
     * @return the bean archive index
     */
    public IndexView getBeanArchiveIndex() {
        return beanArchiveComputingIndex != null ? beanArchiveComputingIndex : beanArchiveImmutableIndex;
    }

    /**
     * This index is optional and is used to discover types during type-safe resolution.
     * <p>
     * Some types may not be part of the bean archive index but are still needed during type-safe resolution.
     *
     * @return the application index or {@code null}
     */
    public IndexView getApplicationIndex() {
        return applicationIndex;
    }

    public BeanResolver getBeanResolver() {
        return beanResolver;
    }

    public BeanResolver getDelegateInjectionPointResolver() {
        return delegateInjectionPointResolver;
    }

    public AssignabilityCheck getAssignabilityCheck() {
        return assignabilityCheck;
    }

    boolean hasApplicationIndex() {
        return applicationIndex != null;
    }

    public InterceptorResolver getInterceptorResolver() {
        return interceptorResolver;
    }

    ClassInfo getQualifier(DotName name) {
        return qualifiers.get(name);
    }

    boolean isInheritedQualifier(DotName name) {
        return (getQualifier(name).declaredAnnotation(DotNames.INHERITED) != null);
    }

    /**
     * Extracts qualifiers from given annotation instance.
     * This returns a collection because in case of repeating qualifiers there can be multiple.
     * For most instances this will be a singleton instance (if given annotation is a qualifier) or an empty list for
     * cases where the annotation is not a qualifier.
     *
     * @param annotation annotation to be inspected
     * @return a collection of qualifiers or an empty collection
     */
    Collection<AnnotationInstance> extractQualifiers(AnnotationInstance annotation) {
        return extractAnnotations(annotation, qualifiers, repeatingQualifierAnnotations);
    }

    /**
     * Extracts interceptor bindings from given annotation instance.
     * This returns a collection because in case of repeating interceptor bindings there can be multiple.
     * For most instances this will be a singleton instance (if given annotation is an interceptor binding) or
     * an empty list for cases where the annotation is not an interceptor binding.
     *
     * @param annotation annotation to be inspected
     * @return a collection of interceptor bindings or an empty collection
     */
    public Collection<AnnotationInstance> extractInterceptorBindings(AnnotationInstance annotation) {
        return extractAnnotations(annotation, interceptorBindings, repeatingInterceptorBindingAnnotations);
    }

    private static Collection<AnnotationInstance> extractAnnotations(AnnotationInstance annotation,
            Map<DotName, ClassInfo> singulars, Map<DotName, ClassInfo> repeatables) {
        if (!annotation.runtimeVisible()) {
            return Collections.emptyList();
        }
        DotName annotationName = annotation.name();
        if (singulars.get(annotationName) != null) {
            return Collections.singleton(annotation);
        } else if (repeatables.get(annotationName) != null) {
            // repeatable, we need to extract actual annotations
            return Annotations.onlyRuntimeVisible(Arrays.asList(annotation.value().asNestedArray()));
        } else {
            // neither singular nor repeatable, return empty collection
            return Collections.emptyList();
        }
    }

    ClassInfo getInterceptorBinding(DotName name) {
        return interceptorBindings.get(name);
    }

    Set<AnnotationInstance> getTransitiveInterceptorBindings(DotName name) {
        return transitiveInterceptorBindings.get(name);
    }

    Map<DotName, Set<AnnotationInstance>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
    }

    StereotypeInfo getStereotype(DotName name) {
        return stereotypes.get(name);
    }

    BeanDefiningAnnotation getBeanDefiningAnnotation(DotName name) {
        return beanDefiningAnnotations.get(name);
    }

    Set<DotName> getResourceAnnotations() {
        return resourceAnnotations;
    }

    AnnotationStore getAnnotationStore() {
        return annotationStore;
    }

    public Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        return annotationStore.getAnnotations(target);
    }

    public AnnotationInstance getAnnotation(AnnotationTarget target, DotName name) {
        return annotationStore.getAnnotation(target, name);
    }

    public boolean hasAnnotation(AnnotationTarget target, DotName name) {
        return annotationStore.hasAnnotation(target, name);
    }

    Map<ScopeInfo, Function<MethodCreator, ResultHandle>> getCustomContexts() {
        return customContexts;
    }

    ScopeInfo getScope(DotName scopeAnnotationName) {
        return getScope(scopeAnnotationName, customContexts);
    }

    /**
     *
     * @param target
     * @param stereotypes
     * @return the computed priority or {@code null}
     */
    Integer computeAlternativePriority(AnnotationTarget target, List<StereotypeInfo> stereotypes) {
        return alternativePriorities != null ? alternativePriorities.compute(target, stereotypes) : null;
    }

    Set<MethodInfo> getObserverAndProducerMethods() {
        Set<MethodInfo> ret = new HashSet<>();
        for (ObserverInfo observer : observers) {
            if (!observer.isSynthetic()) {
                ret.add(observer.getObserverMethod());
            }
        }
        for (BeanInfo bean : beans) {
            if (bean.isProducerMethod()) {
                ret.add(bean.getTarget().get().asMethod());
            }
        }
        return ret;
    }

    private void buildContextPut(String key, Object value) {
        if (buildContext != null) {
            buildContext.putInternal(key, value);
        }
    }

    private boolean isRuntimeAnnotationType(ClassInfo annotationType) {
        AnnotationInstance retention = annotationType.declaredAnnotation(Retention.class);
        return retention != null && "RUNTIME".equals(retention.value().asEnum());
    }

    private Map<DotName, ClassInfo> findQualifiers() {
        Map<DotName, ClassInfo> qualifiers = new HashMap<>();
        for (AnnotationInstance qualifier : beanArchiveImmutableIndex.getAnnotations(DotNames.QUALIFIER)) {
            ClassInfo qualifierClass = qualifier.target().asClass();
            if (!isRuntimeAnnotationType(qualifierClass)) {
                continue;
            }
            if (isExcluded(qualifierClass)) {
                continue;
            }
            qualifiers.put(qualifierClass.name(), qualifierClass);
        }
        return qualifiers;
    }

    private Map<DotName, ClassInfo> findContainerAnnotations(Map<DotName, ClassInfo> annotations) {
        Map<DotName, ClassInfo> containerAnnotations = new HashMap<>();
        for (ClassInfo annotation : annotations.values()) {
            AnnotationInstance repeatableMetaAnnotation = annotation.declaredAnnotation(DotNames.REPEATABLE);
            if (repeatableMetaAnnotation != null) {
                DotName containerAnnotationName = repeatableMetaAnnotation.value().asClass().name();
                ClassInfo containerClass = getClassByName(getBeanArchiveIndex(), containerAnnotationName);
                containerAnnotations.put(containerAnnotationName, containerClass);
            }
        }
        return containerAnnotations;
    }

    private Map<DotName, ClassInfo> findInterceptorBindings() {
        Map<DotName, ClassInfo> bindings = new HashMap<>();
        // Note: doesn't use AnnotationStore, this will operate on classes without applying annotation transformers
        for (AnnotationInstance binding : beanArchiveImmutableIndex.getAnnotations(DotNames.INTERCEPTOR_BINDING)) {
            ClassInfo bindingClass = binding.target().asClass();
            if (!isRuntimeAnnotationType(bindingClass)) {
                continue;
            }
            if (isExcluded(bindingClass)) {
                continue;
            }
            bindings.put(bindingClass.name(), bindingClass);
        }
        return bindings;
    }

    private static Map<DotName, Set<AnnotationInstance>> findTransitiveInterceptorBindings(Collection<DotName> initialBindings,
            Map<DotName, Set<AnnotationInstance>> result, Map<DotName, ClassInfo> interceptorBindings,
            AnnotationStore annotationStore) {
        // for all known interceptor bindings
        for (DotName annotationName : initialBindings) {
            Set<AnnotationInstance> transitiveBindings = new HashSet<>();
            // for all annotations on them; use AnnotationStore to have up-to-date info
            for (AnnotationInstance bindingCandidate : annotationStore
                    .getAnnotations(interceptorBindings.get(annotationName))) {
                // if the annotation is an interceptor binding itself
                // Note: this verifies it against bindings found without application of transformers
                if (interceptorBindings.get(bindingCandidate.name()) != null) {
                    // register as transitive binding
                    transitiveBindings.add(bindingCandidate);
                }
            }
            if (!transitiveBindings.isEmpty()) {
                result.computeIfAbsent(annotationName, k -> new HashSet<>()).addAll(transitiveBindings);
            }
        }
        // now iterate over all so we can put together list for arbitrary transitive depth
        for (DotName name : result.keySet()) {
            result.put(name, recursiveBuild(name, result));
        }
        return result;
    }

    private static Set<AnnotationInstance> recursiveBuild(DotName name,
            Map<DotName, Set<AnnotationInstance>> transitiveBindingsMap) {
        Set<AnnotationInstance> result = transitiveBindingsMap.get(name);
        for (AnnotationInstance instance : transitiveBindingsMap.get(name)) {
            if (transitiveBindingsMap.containsKey(instance.name())) {
                // recursively find
                result.addAll(recursiveBuild(instance.name(), transitiveBindingsMap));
            }
        }
        return result;
    }

    private Map<DotName, StereotypeInfo> findStereotypes(Map<DotName, ClassInfo> interceptorBindings,
            Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts,
            Set<DotName> additionalStereotypes, AnnotationStore annotationStore) {

        Map<DotName, StereotypeInfo> stereotypes = new HashMap<>();

        Set<DotName> stereotypeNames = new HashSet<>();
        for (AnnotationInstance annotation : beanArchiveImmutableIndex.getAnnotations(DotNames.STEREOTYPE)) {
            stereotypeNames.add(annotation.target().asClass().name());
        }
        stereotypeNames.addAll(additionalStereotypes);

        for (DotName stereotypeName : stereotypeNames) {
            ClassInfo stereotypeClass = getClassByName(getBeanArchiveIndex(), stereotypeName);
            if (stereotypeClass != null && !isExcluded(stereotypeClass)) {
                if (!isRuntimeAnnotationType(stereotypeClass)) {
                    continue;
                }

                boolean isAlternative = false;
                Integer alternativePriority = null;
                Set<ScopeInfo> scopes = new HashSet<>();
                List<AnnotationInstance> bindings = new ArrayList<>();
                List<AnnotationInstance> parentStereotypes = new ArrayList<>();
                boolean isNamed = false;

                for (AnnotationInstance annotation : annotationStore.getAnnotations(stereotypeClass)) {
                    if (DotNames.ALTERNATIVE.equals(annotation.name())) {
                        isAlternative = true;
                    } else if (interceptorBindings.containsKey(annotation.name())) {
                        bindings.add(annotation);
                    } else if (stereotypeNames.contains(annotation.name())) {
                        parentStereotypes.add(annotation);
                    } else if (DotNames.NAMED.equals(annotation.name())) {
                        if (annotation.value() != null && !annotation.value()
                                .asString()
                                .isEmpty()) {
                            throw new DefinitionException(
                                    "Stereotype must not declare @Named with a non-empty value: " + stereotypeClass);
                        }
                        isNamed = true;
                    } else if (DotNames.PRIORITY.equals(annotation.name())) {
                        alternativePriority = annotation.value().asInt();
                    } else if (DotNames.ARC_PRIORITY.equals(annotation.name()) && alternativePriority == null) {
                        alternativePriority = annotation.value().asInt();
                    } else {
                        final ScopeInfo scope = getScope(annotation.name(), customContexts);
                        if (scope != null) {
                            scopes.add(scope);
                        }
                    }
                }
                boolean isAdditionalStereotype = additionalStereotypes.contains(stereotypeName);
                final ScopeInfo scope = getValidScope(scopes, stereotypeClass);
                boolean isInherited = stereotypeClass.declaredAnnotation(DotNames.INHERITED) != null;
                stereotypes.put(stereotypeName, new StereotypeInfo(scope, bindings, isAlternative, alternativePriority,
                        isNamed, isAdditionalStereotype, stereotypeClass, isInherited, parentStereotypes));
            }
        }
        return stereotypes;
    }

    private static ScopeInfo getScope(DotName scopeAnnotationName,
            Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts) {
        BuiltinScope builtin = BuiltinScope.from(scopeAnnotationName);
        if (builtin != null) {
            return builtin.getInfo();
        }
        for (ScopeInfo customScope : customContexts.keySet()) {
            if (customScope.getDotName().equals(scopeAnnotationName)) {
                return customScope;
            }
        }
        return null;
    }

    static ScopeInfo getValidScope(Set<ScopeInfo> scopes, AnnotationTarget target) {
        switch (scopes.size()) {
            case 0:
                return null;
            case 1:
                return scopes.iterator().next();
            default:
                throw new DefinitionException(
                        "Different scopes defined for: " + target + "; scopes: " + scopes.stream().map(ScopeInfo::getDotName)
                                .map(DotName::toString).collect(Collectors.joining(", ")));
        }
    }

    private List<BeanInfo> findBeans(Collection<DotName> beanDefiningAnnotations, List<ObserverInfo> observers,
            List<InjectionPointInfo> injectionPoints, boolean jtaCapabilities) {

        Set<ClassInfo> beanClasses = new HashSet<>();
        Set<MethodInfo> producerMethods = new HashSet<>();
        Set<MethodInfo> disposerMethods = new HashSet<>();
        Set<FieldInfo> producerFields = new HashSet<>();
        Map<MethodInfo, Set<ClassInfo>> syncObserverMethods = new HashMap<>();
        Map<MethodInfo, Set<ClassInfo>> asyncObserverMethods = new HashMap<>();
        // Stereotypes excluding additional BeanDefiningAnnotations
        Set<DotName> realStereotypes = this.stereotypes.values().stream()
                .filter(StereotypeInfo::isGenuine)
                .map(StereotypeInfo::getName)
                .collect(Collectors.toSet());

        // If needed use the specialized immutable index to discover beans
        for (ClassInfo beanClass : beanArchiveImmutableIndex.getKnownClasses()) {

            if (Modifier.isInterface(beanClass.flags()) || Modifier.isAbstract(beanClass.flags())
                    || beanClass.isAnnotation() || beanClass.isEnum()) {
                // Skip interfaces, abstract classes, annotations and enums
                continue;
            }

            if (beanClass.nestingType().equals(NestingType.ANONYMOUS) || beanClass.nestingType().equals(NestingType.LOCAL)
                    || (beanClass.nestingType().equals(NestingType.INNER) && !Modifier.isStatic(beanClass.flags()))) {
                // Skip anonymous, local and inner classes
                continue;
            }

            if (isExcluded(beanClass)) {
                continue;
            }

            if (!beanClass.hasNoArgsConstructor()) {
                int numberOfConstructorsWithoutInject = 0;
                int numberOfConstructorsWithInject = 0;
                for (MethodInfo m : beanClass.methods()) {
                    if (m.name().equals(Methods.INIT)) {
                        if (annotationStore.hasAnnotation(m, DotNames.INJECT)) {
                            numberOfConstructorsWithInject++;
                        } else {
                            numberOfConstructorsWithoutInject++;
                        }
                    }
                }

                // in strict compatibility mode, the bean needs to have either no args ctor or some with @Inject
                // note that we perform validation (for multiple ctors for instance) later in the cycle
                if (strictCompatibility && numberOfConstructorsWithInject == 0) {
                    continue;
                }

                // without strict compatibility, a bean without no-arg constructor needs to have either a constructor
                // annotated with @Inject or a single constructor
                if (numberOfConstructorsWithInject == 0 && numberOfConstructorsWithoutInject != 1) {
                    continue;
                }
            }

            if (isVetoed(beanClass)) {
                // Skip vetoed bean classes
                continue;
            }

            if (annotationStore.hasAnnotation(beanClass, DotNames.INTERCEPTOR)
                    || annotationStore.hasAnnotation(beanClass, DotNames.DECORATOR)) {
                // Skip interceptors and decorators
                continue;
            }

            if (beanClass.interfaceNames().contains(DotNames.EXTENSION)) {
                // Skip portable extensions
                continue;
            }

            if (beanClass.interfaceNames().contains(DotNames.BUILD_COMPATIBLE_EXTENSION)) {
                // Skip build compatible extensions
                continue;
            }

            boolean hasBeanDefiningAnnotation = false;
            if (annotationStore.hasAnyAnnotation(beanClass, beanDefiningAnnotations)) {
                hasBeanDefiningAnnotation = true;
                beanClasses.add(beanClass);
            }

            // non-inherited methods
            for (MethodInfo method : beanClass.methods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                if (annotationStore.getAnnotations(method).isEmpty()) {
                    continue;
                }
                if (annotationStore.hasAnnotation(method, DotNames.PRODUCES)
                        && !annotationStore.hasAnnotation(method, DotNames.VETOED_PRODUCER)) {
                    // Do not register classes with producers and no bean def. annotation as beans in strict mode
                    // Producers are not inherited
                    if (strictCompatibility) {
                        if (hasBeanDefiningAnnotation) {
                            producerMethods.add(method);
                        }
                    } else {
                        producerMethods.add(method);
                        if (!hasBeanDefiningAnnotation) {
                            LOGGER.debugf("Producer method found but %s has no bean defining annotation - using @Dependent",
                                    beanClass);
                            beanClasses.add(beanClass);
                        }
                    }
                }
                if (annotationStore.hasAnnotation(method, DotNames.DISPOSES)) {
                    // Disposers are not inherited
                    disposerMethods.add(method);
                }
            }

            // inherited methods
            ClassInfo aClass = beanClass;
            Set<Methods.MethodKey> methods = new HashSet<>();
            while (aClass != null) {
                for (MethodInfo method : aClass.methods()) {
                    Methods.MethodKey methodDescriptor = new Methods.MethodKey(method);
                    if (method.isSynthetic() || Methods.isOverriden(methodDescriptor, methods)) {
                        continue;
                    }
                    methods.add(methodDescriptor);
                    Collection<AnnotationInstance> methodAnnotations = annotationStore.getAnnotations(method);
                    if (methodAnnotations.isEmpty()) {
                        continue;
                    }
                    // Verify that non-producer methods are not annotated with stereotypes
                    // only account for 'real' stereotypes that are not additional BeanDefiningAnnotations
                    if (!annotationStore.hasAnnotation(method, DotNames.PRODUCES)) {
                        for (AnnotationInstance i : methodAnnotations) {
                            if (realStereotypes.contains(i.name())) {
                                throw new DefinitionException(
                                        "Method " + method + " of class " + beanClass
                                                + " is not a producer method, but is annotated " +
                                                "with a stereotype: " + i.name().toString());
                            }
                        }
                    }
                    if (annotationStore.hasAnnotation(method, DotNames.OBSERVES)) {
                        syncObserverMethods.computeIfAbsent(method, ignored -> new HashSet<>())
                                .add(beanClass);
                        // add only concrete classes
                        if (!Modifier.isAbstract(beanClass.flags())) {
                            // do not register classes with observers and no bean def. annotation as beans in strict mode
                            if (!strictCompatibility) {
                                beanClasses.add(beanClass);
                                if (!hasBeanDefiningAnnotation) {
                                    LOGGER.debugf(
                                            "Observer method found but %s has no bean defining annotation - using @Dependent",
                                            beanClass);
                                }
                            }
                        }
                    } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                        asyncObserverMethods.computeIfAbsent(method, ignored -> new HashSet<>())
                                .add(beanClass);
                        // add only concrete classes
                        if (!Modifier.isAbstract(beanClass.flags())) {
                            // do not register classes with observers and no bean def. annotation as beans in strict mode
                            if (!strictCompatibility) {
                                beanClasses.add(beanClass);
                                if (!hasBeanDefiningAnnotation) {
                                    LOGGER.debugf(
                                            "Observer method found but %s has no bean defining annotation - using @Dependent",
                                            beanClass);
                                }
                            }
                        }
                    }
                }
                DotName superType = aClass.superName();
                aClass = superType != null && !superType.equals(DotNames.OBJECT)
                        ? getClassByName(getBeanArchiveIndex(), superType)
                        : null;
            }

            // non-inherited fields
            for (FieldInfo field : beanClass.fields()) {
                if (annotationStore.hasAnnotation(field, DotNames.PRODUCES)
                        && !annotationStore.hasAnnotation(field, DotNames.VETOED_PRODUCER)) {
                    if (annotationStore.hasAnnotation(field, DotNames.INJECT)) {
                        throw new DefinitionException("Injected field cannot be annotated with @Produces: " + field);
                    }
                    // Do not register classes with producers and no bean def. annotation as beans in strict mode
                    // Producer fields are not inherited
                    if (strictCompatibility) {
                        if (hasBeanDefiningAnnotation) {
                            producerFields.add(field);
                        }
                    } else {
                        producerFields.add(field);
                        if (!hasBeanDefiningAnnotation) {
                            LOGGER.debugf("Producer field found but %s has no bean defining annotation - using @Dependent",
                                    beanClass);
                            beanClasses.add(beanClass);
                        }
                    }
                } else {
                    // Verify that non-producer fields are not annotated with stereotypes
                    for (AnnotationInstance i : annotationStore.getAnnotations(field)) {
                        if (realStereotypes.contains(i.name())) {
                            throw new DefinitionException(
                                    "Field " + field + " of class " + beanClass
                                            + " is not a producer field, but is annotated " +
                                            "with a stereotype: " + i.name().toString());
                        }
                    }
                }
            }
        }

        // Build metadata for typesafe resolution
        List<BeanInfo> beans = new ArrayList<>();
        Map<ClassInfo, BeanInfo> beanClassToBean = new HashMap<>();
        for (ClassInfo beanClass : beanClasses) {
            BeanInfo classBean = Beans.createClassBean(beanClass, this, injectionPointTransformer);
            if (classBean != null) {
                beans.add(classBean);
                beanClassToBean.put(beanClass, classBean);
                injectionPoints.addAll(classBean.getAllInjectionPoints());

                // specification requires to disallow non-static public fields on non-`@Dependent` beans,
                // but we do what Weld does: only disallow them on normal scoped beans (disallowing them
                // on `@Singleton` beans would actually prevent passing the AtInject TCK)
                //
                // only check this in the strictly compatible mode, as this is pretty invasive,
                // it even prevents public producer fields
                if (classBean.getScope().isNormal() && strictCompatibility) {
                    ClassInfo aClass = beanClass;
                    while (aClass != null) {
                        for (FieldInfo field : aClass.fields()) {
                            if (Modifier.isPublic(field.flags()) && !Modifier.isStatic(field.flags())) {
                                throw new DefinitionException("Non-static public field " + field
                                        + " present on normal scoped bean " + beanClass);
                            }
                        }

                        DotName superClass = aClass.superName();
                        aClass = superClass != null && !superClass.equals(DotNames.OBJECT)
                                ? getClassByName(getBeanArchiveIndex(), superClass)
                                : null;
                    }
                }
            }
        }

        List<DisposerInfo> disposers = new ArrayList<>();
        for (MethodInfo disposerMethod : disposerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(disposerMethod.declaringClass());
            if (declaringBean != null) {
                Injection injection = Injection.forDisposer(disposerMethod, declaringBean.getImplClazz(), this,
                        injectionPointTransformer);
                injection.init(declaringBean);
                disposers.add(new DisposerInfo(declaringBean, disposerMethod, injection));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }
        Set<DisposerInfo> unusedDisposers = new HashSet<>(disposers);

        for (MethodInfo producerMethod : producerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(producerMethod.declaringClass());
            if (declaringBean != null) {
                Set<Type> beanTypes = Types.getProducerMethodTypeClosure(producerMethod, this);
                DisposerInfo disposer = findDisposer(beanTypes, declaringBean, producerMethod, disposers);
                unusedDisposers.remove(disposer);
                BeanInfo producerMethodBean = Beans.createProducerMethod(beanTypes, producerMethod, declaringBean, this,
                        disposer, injectionPointTransformer);
                if (producerMethodBean != null) {
                    beans.add(producerMethodBean);
                    injectionPoints.addAll(producerMethodBean.getAllInjectionPoints());
                }
            }
        }

        for (FieldInfo producerField : producerFields) {
            BeanInfo declaringBean = beanClassToBean.get(producerField.declaringClass());
            if (declaringBean != null) {
                Set<Type> beanTypes = Types.getProducerFieldTypeClosure(producerField, this);
                DisposerInfo disposer = findDisposer(beanTypes, declaringBean, producerField, disposers);
                unusedDisposers.remove(disposer);
                BeanInfo producerFieldBean = Beans.createProducerField(producerField, declaringBean, this,
                        disposer);
                if (producerFieldBean != null) {
                    beans.add(producerFieldBean);
                }
            }
        }

        // we track unused disposers to make this validation cheaper: no need to validate disposers
        // that are used, clearly a matching producer bean exists for those
        for (DisposerInfo unusedDisposer : unusedDisposers) {
            Type disposedParamType = unusedDisposer.getDisposedParameterType();
            boolean matchingProducerBeanExists = false;
            scan: for (BeanInfo bean : beans) {
                if (bean.isProducer()) {
                    if (!bean.getDeclaringBean().equals(unusedDisposer.getDeclaringBean())) {
                        continue;
                    }
                    for (Type beanType : bean.getTypes()) {
                        if (beanResolver.matches(disposedParamType, beanType)) {
                            matchingProducerBeanExists = true;
                            break scan;
                        }
                    }
                }
            }
            if (!matchingProducerBeanExists) {
                throw new DefinitionException("No producer method or field declared by the bean class that is assignable "
                        + "to the disposed parameter of a disposer method: " + unusedDisposer.getDisposerMethod());
            }
        }

        for (Map.Entry<MethodInfo, Set<ClassInfo>> entry : syncObserverMethods.entrySet()) {
            registerObserverMethods(entry.getValue(), observers, injectionPoints,
                    beanClassToBean, entry.getKey(), false, observerTransformers, jtaCapabilities);
        }

        for (Map.Entry<MethodInfo, Set<ClassInfo>> entry : asyncObserverMethods.entrySet()) {
            registerObserverMethods(entry.getValue(), observers, injectionPoints,
                    beanClassToBean, entry.getKey(), true, observerTransformers, jtaCapabilities);
        }

        if (LOGGER.isTraceEnabled()) {
            for (BeanInfo bean : beans) {
                LOGGER.logf(Level.TRACE, "Created %s", bean);
            }
        }
        return beans;
    }

    private boolean isVetoed(ClassInfo beanClass) {
        if (annotationStore.hasAnnotation(beanClass, DotNames.VETOED)) {
            return true;
        }

        // using immutable index, because we expect that if class is discovered,
        // the whole package is indexed (otherwise we'd get a lot of warnings that
        // package-info.class couldn't be loaded during on-demand indexing)
        String packageName = beanClass.name().packagePrefix();
        org.jboss.jandex.ClassInfo packageClass = beanArchiveImmutableIndex.getClassByName(
                DotName.createSimple(packageName + ".package-info"));
        return packageClass != null && annotationStore.hasAnnotation(packageClass, DotNames.VETOED);
    }

    private boolean isExcluded(ClassInfo beanClass) {
        if (!excludeTypes.isEmpty()) {
            for (Predicate<ClassInfo> exclude : excludeTypes) {
                if (exclude.test(beanClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void registerObserverMethods(Collection<ClassInfo> beanClasses,
            List<ObserverInfo> observers,
            List<InjectionPointInfo> injectionPoints,
            Map<ClassInfo, BeanInfo> beanClassToBean,
            MethodInfo observerMethod,
            boolean async, List<ObserverTransformer> observerTransformers,
            boolean jtaCapabilities) {

        for (ClassInfo beanClass : beanClasses) {
            BeanInfo declaringBean = beanClassToBean.get(beanClass);
            if (declaringBean != null) {
                Injection injection = Injection.forObserver(observerMethod, declaringBean.getImplClazz(), this,
                        injectionPointTransformer);
                injection.init(declaringBean);
                ObserverInfo observer = ObserverInfo.create(declaringBean, observerMethod, injection, async,
                        observerTransformers, buildContext, jtaCapabilities);
                if (observer != null) {
                    observers.add(observer);
                    injectionPoints.addAll(injection.injectionPoints);
                }
            }
        }
    }

    private DisposerInfo findDisposer(Set<Type> beanTypes, BeanInfo declaringBean, AnnotationTarget producer,
            List<DisposerInfo> disposers) {
        // we don't have a `BeanInfo` for the producer yet (the outcome of this method is used to build it),
        // so we need to construct its set of qualifiers manually
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        // ignore annotations on producer method parameters -- they may be injection point qualifiers
        for (AnnotationInstance annotation : Annotations.getAnnotations(producer.kind(), getAnnotations(producer))) {
            qualifiers.addAll(extractQualifiers(annotation));
        }
        Beans.addImplicitQualifiers(qualifiers); // need to consider `@Any` (and possibly `@Default`) too

        List<DisposerInfo> found = new ArrayList<>();
        for (DisposerInfo disposer : disposers) {
            if (disposer.getDeclaringBean().equals(declaringBean)) {
                boolean hasQualifier = true;
                for (AnnotationInstance disposerQualifier : disposer.getDisposedParameterQualifiers()) {
                    if (!Beans.hasQualifier(declaringBean.getDeployment(), disposerQualifier, qualifiers)) {
                        hasQualifier = false;
                    }
                }
                if (hasQualifier) {
                    Type disposedParamType = disposer.getDisposedParameterType();
                    for (Type beanType : beanTypes) {
                        if (beanResolver.matches(disposedParamType, beanType)) {
                            found.add(disposer);
                            break;
                        }
                    }
                }
            }
        }
        if (found.size() > 1) {
            throw new DefinitionException("Multiple disposer methods found for " + producer);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    // keep it public we need this method in quarkus integration
    public static Set<DotName> initBeanDefiningAnnotations(Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            Set<DotName> stereotypes) {
        Set<DotName> beanDefiningAnnotations = new HashSet<>();
        for (BuiltinScope scope : BuiltinScope.values()) {
            beanDefiningAnnotations.add(scope.getInfo().getDotName());
        }
        if (additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation additional : additionalBeanDefiningAnnotations) {
                beanDefiningAnnotations.add(additional.getAnnotation());
            }
        }
        beanDefiningAnnotations.addAll(stereotypes);
        return beanDefiningAnnotations;
    }

    private RegistrationContext registerSyntheticBeans(List<BeanRegistrar> beanRegistrars, BuildContext buildContext) {
        BeanRegistrationContextImpl context = new BeanRegistrationContextImpl(buildContext, this);
        for (BeanRegistrar registrar : beanRegistrars) {
            registrar.register(context);
        }
        if (buildCompatibleExtensions != null) {
            buildCompatibleExtensions.runSynthesis(beanArchiveComputingIndex);
            buildCompatibleExtensions.registerSyntheticBeans(context);
        }
        this.injectionPoints.addAll(context.syntheticInjectionPoints);
        return context;
    }

    io.quarkus.arc.processor.ObserverRegistrar.RegistrationContext registerSyntheticObservers(
            List<ObserverRegistrar> observerRegistrars) {
        ObserverRegistrationContextImpl context = new ObserverRegistrationContextImpl(buildContext, this);
        for (ObserverRegistrar registrar : observerRegistrars) {
            context.extension = registrar;
            registrar.register(context);
            context.extension = null;
        }
        if (buildCompatibleExtensions != null) {
            buildCompatibleExtensions.registerSyntheticObservers(context);
            buildCompatibleExtensions.runRegistrationAgain(beanArchiveComputingIndex, beans, observers);
        }
        return context;
    }

    private void addSyntheticBean(BeanInfo bean) {
        for (BeanInfo b : beans) {
            if (b.getIdentifier().equals(bean.getIdentifier())) {
                throw new IllegalStateException(
                        "A synthetic bean with identifier " + bean.getIdentifier() + " is already registered: "
                                + b);
            }
        }
        beans.add(bean);
    }

    private void addSyntheticObserver(ObserverConfigurator configurator) {
        observers.add(ObserverInfo.create(configurator.id, this, configurator.beanClass, null, null, null, null,
                configurator.observedType,
                configurator.observedQualifiers,
                Reception.ALWAYS, configurator.transactionPhase, configurator.isAsync, configurator.priority,
                observerTransformers, buildContext,
                jtaCapabilities, configurator.notifyConsumer, configurator.params));
    }

    static void processErrors(List<Throwable> errors) {
        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                Throwable error = errors.get(0);
                if (error instanceof DeploymentException) {
                    throw (DeploymentException) error;
                } else if (error instanceof DefinitionException) {
                    throw (DefinitionException) error;
                } else {
                    throw new DeploymentException(errors.get(0));
                }
            } else {
                StringBuilder message = new StringBuilder("Found " + errors.size() + " deployment problems: ");
                int idx = 1;
                for (Throwable error : errors) {
                    message.append("\n").append("[").append(idx++).append("] ").append(error.getMessage());
                }
                DeploymentException deploymentException = new DeploymentException(message.toString());
                for (Throwable error : errors) {
                    deploymentException.addSuppressed(error);
                }
                throw deploymentException;
            }
        }
    }

    private List<InterceptorInfo> findInterceptors(List<InjectionPointInfo> injectionPoints) {
        Map<DotName, ClassInfo> interceptorClasses = new HashMap<>();
        for (AnnotationInstance annotation : beanArchiveImmutableIndex.getAnnotations(DotNames.INTERCEPTOR)) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                interceptorClasses.put(annotation.target().asClass().name(), annotation.target().asClass());
            }
        }
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (ClassInfo interceptorClass : interceptorClasses.values()) {
            if (isVetoed(interceptorClass) || isExcluded(interceptorClass)) {
                // Skip vetoed interceptors
                continue;
            }
            InterceptorInfo interceptor = Interceptors.createInterceptor(interceptorClass, this, injectionPointTransformer);
            if (interceptor != null) {
                interceptors.add(interceptor);
            }
        }
        if (LOGGER.isTraceEnabled()) {
            for (InterceptorInfo interceptor : interceptors) {
                LOGGER.logf(Level.TRACE, "Created %s", interceptor);
            }
        }
        for (InterceptorInfo interceptor : interceptors) {
            injectionPoints.addAll(interceptor.getAllInjectionPoints());
        }
        return interceptors;
    }

    private List<DecoratorInfo> findDecorators(List<InjectionPointInfo> injectionPoints) {
        Map<DotName, ClassInfo> decoratorClasses = new HashMap<>();
        for (AnnotationInstance annotation : beanArchiveImmutableIndex.getAnnotations(DotNames.DECORATOR)) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                decoratorClasses.put(annotation.target().asClass().name(), annotation.target().asClass());
            }
        }
        List<DecoratorInfo> decorators = new ArrayList<>();
        for (ClassInfo decoratorClass : decoratorClasses.values()) {
            if (isVetoed(decoratorClass) || isExcluded(decoratorClass)) {
                // Skip vetoed decorators
                continue;
            }
            DecoratorInfo decorator = Decorators.createDecorator(decoratorClass, this, injectionPointTransformer);
            if (decorator != null) {
                decorators.add(decorator);
            }
        }
        if (LOGGER.isTraceEnabled()) {
            for (DecoratorInfo decorator : decorators) {
                LOGGER.logf(Level.TRACE, "Created %s", decorator);
            }
        }
        for (DecoratorInfo decorator : decorators) {
            injectionPoints.addAll(decorator.getAllInjectionPoints());
        }
        return decorators;
    }

    private void validateInterceptorsAndDecorators(List<Throwable> errors,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        for (InterceptorInfo interceptor : interceptors) {
            interceptor.validateInterceptorDecorator(errors, bytecodeTransformerConsumer);
        }
        for (DecoratorInfo decorator : decorators) {
            decorator.validateInterceptorDecorator(errors, bytecodeTransformerConsumer);
        }
    }

    private void validateBeans(List<Throwable> errors, Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {

        Set<String> namespaces = new HashSet<>();
        Map<String, List<BeanInfo>> namedBeans = new HashMap<>();
        Set<DotName> classesReceivingNoArgsCtor = new HashSet<>();

        for (BeanInfo bean : beans) {
            if (bean.getName() != null) {
                List<BeanInfo> named = namedBeans.get(bean.getName());
                if (named == null) {
                    named = new ArrayList<>();
                    namedBeans.put(bean.getName(), named);
                }
                named.add(bean);
                findNamespaces(bean, namespaces);
            }
            bean.validate(errors, bytecodeTransformerConsumer, classesReceivingNoArgsCtor);
        }

        if (!namedBeans.isEmpty()) {
            for (Entry<String, List<BeanInfo>> entry : namedBeans.entrySet()) {
                String name = entry.getKey();

                if (entry.getValue().size() > 1) {
                    if (Beans.resolveAmbiguity(entry.getValue()) == null) {
                        errors.add(new DeploymentException("Unresolvable ambiguous bean name detected: " + name
                                + "\nBeans:\n" + entry.getValue()
                                        .stream()
                                        .map(Object::toString)
                                        .collect(Collectors.joining("\n"))));
                    }
                }

                if (strictCompatibility && namespaces.contains(name)) {
                    errors.add(new DeploymentException(
                            "Bean name '" + name + "' is identical to a bean name prefix used elsewhere"));
                }
            }
        }
    }

    private void findNamespaces(BeanInfo bean, Set<String> namespaces) {
        if (!strictCompatibility) {
            return;
        }

        if (bean.getName() != null) {
            String[] parts = bean.getName().split("\\.");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    StringBuilder builder = new StringBuilder();
                    for (int j = 0; j <= i; j++) {
                        if (j > 0) {
                            builder.append('.');
                        }
                        builder.append(parts[j]);
                    }
                    namespaces.add(builder.toString());
                }
            }
        }
    }

    /**
     * Returns the set of names of non-binding annotation members of given interceptor
     * binding annotation that was registered through {@code InterceptorBindingRegistrar}.
     * <p>
     * Does <em>not</em> return non-binding members of interceptor bindings that were
     * discovered based on the {@code @InterceptorBinding} annotation; in such case,
     * one has to manually check presence of the {@code @NonBinding} annotation on
     * the annotation member declaration.
     *
     * @param name name of the interceptor binding annotation that was registered through
     *        {@code InterceptorBindingRegistrar}
     * @return set of non-binding annotation members of the interceptor binding annotation
     */
    public Set<String> getInterceptorNonbindingMembers(DotName name) {
        return interceptorNonbindingMembers.getOrDefault(name, Collections.emptySet());
    }

    /**
     * Returns the set of names of non-binding annotation members of given qualifier
     * annotation that was registered through {@code QualifierRegistrar}.
     * <p>
     * Does <em>not</em> return non-binding members of interceptor bindings that were
     * discovered based on the {@code @Qualifier} annotation; in such case, one has to
     * manually check presence of the {@code @NonBinding} annotation on the annotation member
     * declaration.
     *
     * @param name name of the qualifier annotation that was registered through
     *        {@code QualifierRegistrar}
     * @return set of non-binding annotation members of the qualifier annotation
     */
    public Set<String> getQualifierNonbindingMembers(DotName name) {
        return qualifierNonbindingMembers.getOrDefault(name, Collections.emptySet());
    }

    @Override
    public String toString() {
        return "BeanDeployment [name=" + name + "]";
    }

    private static class ValidationContextImpl implements ValidationContext {

        private final BuildContext buildContext;

        private final List<Throwable> errors;

        public ValidationContextImpl(BuildContext buildContext) {
            this.buildContext = buildContext;
            this.errors = new ArrayList<Throwable>();
        }

        @Override
        public <V> V get(Key<V> key) {
            return buildContext.get(key);
        }

        @Override
        public <V> V put(Key<V> key, V value) {
            return buildContext.put(key, value);
        }

        @Override
        public void addDeploymentProblem(Throwable problem) {
            errors.add(problem);
        }

        @Override
        public List<Throwable> getDeploymentProblems() {
            return Collections.unmodifiableList(errors);
        }

        @Override
        public BeanStream beans() {
            return new BeanStream(get(BuildExtension.Key.BEANS));
        }

        @Override
        public BeanStream removedBeans() {
            return new BeanStream(get(BuildExtension.Key.REMOVED_BEANS));
        }

    }

    private static class BeanRegistrationContextImpl extends RegistrationContextImpl
            implements RegistrationContext, Consumer<BeanInfo> {

        final List<InjectionPointInfo> syntheticInjectionPoints;

        BeanRegistrationContextImpl(BuildContext buildContext, BeanDeployment beanDeployment) {
            super(buildContext, beanDeployment);
            this.syntheticInjectionPoints = new ArrayList<>();
        }

        @Override
        public <T> BeanConfigurator<T> configure(DotName beanClassName) {
            return new BeanConfigurator<T>(beanClassName, beanDeployment, this);
        }

        @Override
        public void accept(BeanInfo bean) {
            beanDeployment.addSyntheticBean(bean);
            syntheticInjectionPoints.addAll(bean.getAllInjectionPoints());
        }

    }

    private static class ObserverRegistrationContextImpl extends RegistrationContextImpl
            implements io.quarkus.arc.processor.ObserverRegistrar.RegistrationContext {

        ObserverRegistrationContextImpl(BuildContext buildContext, BeanDeployment beanDeployment) {
            super(buildContext, beanDeployment);
        }

        @Override
        public ObserverConfigurator configure() {
            ObserverConfigurator configurator = new ObserverConfigurator(beanDeployment::addSyntheticObserver);
            if (extension != null) {
                // Extension may be null if called directly from the ObserverRegistrationPhaseBuildItem
                configurator.beanClass(DotName.createSimple(extension.getClass().getName()));
            }
            return configurator;
        }

        @Override
        public BeanStream beans() {
            return new BeanStream(get(BuildExtension.Key.BEANS));
        }

    }

    static abstract class RegistrationContextImpl implements BuildContext {

        protected final BuildContext parent;
        protected final BeanDeployment beanDeployment;
        protected BuildExtension extension;

        RegistrationContextImpl(BuildContext buildContext, BeanDeployment beanDeployment) {
            this.parent = buildContext;
            this.beanDeployment = beanDeployment;
        }

        @Override
        public <V> V get(Key<V> key) {
            return parent.get(key);
        }

        @Override
        public <V> V put(Key<V> key, V value) {
            return parent.put(key, value);
        }

        public BeanStream beans() {
            return new BeanStream(get(BuildExtension.Key.BEANS));
        }

    }

}
