package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.BeanProcessor.initAndSort;
import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BeanProcessor.BuildContextImpl;
import io.quarkus.arc.processor.BeanRegistrar.RegistrationContext;
import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import java.lang.annotation.Annotation;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
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

public class BeanDeployment {

    private static final Logger LOGGER = Logger.getLogger(BeanDeployment.class);

    private final BuildContextImpl buildContext;

    private final IndexView beanArchiveIndex;

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

    private final Collection<BeanDefiningAnnotation> beanDefiningAnnotations;

    final boolean transformUnproxyableClasses;

    private final boolean jtaCapabilities;

    private final AlternativePriorities alternativePriorities;

    private final List<Predicate<ClassInfo>> excludeTypes;

    BeanDeployment(BuildContextImpl buildContext, BeanProcessor.Builder builder) {
        this.buildContext = buildContext;
        Set<BeanDefiningAnnotation> beanDefiningAnnotations = new HashSet<>();
        if (builder.additionalBeanDefiningAnnotations != null) {
            beanDefiningAnnotations.addAll(builder.additionalBeanDefiningAnnotations);
        }
        this.beanDefiningAnnotations = beanDefiningAnnotations;
        this.resourceAnnotations = new HashSet<>(builder.resourceAnnotations);
        this.beanArchiveIndex = builder.beanArchiveIndex;
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
        this.removedBeans = new CopyOnWriteArraySet<>();
        this.customContexts = new ConcurrentHashMap<>();

        this.excludeTypes = builder.excludeTypes != null ? new ArrayList<>(builder.excludeTypes) : Collections.emptyList();

        qualifierNonbindingMembers = new HashMap<>();
        qualifiers = findQualifiers(this.beanArchiveIndex);
        for (QualifierRegistrar registrar : builder.qualifierRegistrars) {
            for (Map.Entry<DotName, Set<String>> entry : registrar.getAdditionalQualifiers().entrySet()) {
                DotName dotName = entry.getKey();
                ClassInfo classInfo = getClassByName(this.beanArchiveIndex, dotName);
                if (classInfo != null) {
                    if (entry.getValue() != null) {
                        qualifierNonbindingMembers.put(dotName, entry.getValue());
                    }
                    this.qualifiers.put(dotName, classInfo);
                }
            }
        }
        repeatingQualifierAnnotations = findContainerAnnotations(qualifiers, this.beanArchiveIndex);
        buildContextPut(Key.QUALIFIERS.asString(), Collections.unmodifiableMap(qualifiers));

        interceptorNonbindingMembers = new HashMap<>();
        interceptorBindings = findInterceptorBindings(this.beanArchiveIndex);
        for (InterceptorBindingRegistrar registrar : builder.interceptorBindingRegistrars) {
            for (InterceptorBindingRegistrar.InterceptorBinding binding : registrar.getAdditionalBindings()) {
                DotName dotName = binding.getName();
                ClassInfo annotationClass = getClassByName(this.beanArchiveIndex, dotName);
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
        repeatingInterceptorBindingAnnotations = findContainerAnnotations(interceptorBindings, this.beanArchiveIndex);
        buildContextPut(Key.INTERCEPTOR_BINDINGS.asString(), Collections.unmodifiableMap(interceptorBindings));

        this.stereotypes = findStereotypes(this.beanArchiveIndex, interceptorBindings, beanDefiningAnnotations, customContexts,
                builder.additionalStereotypes, annotationStore);
        buildContextPut(Key.STEREOTYPES.asString(), Collections.unmodifiableMap(stereotypes));

        this.transitiveInterceptorBindings = findTransitiveInterceptorBindings(interceptorBindings.keySet(),
                this.beanArchiveIndex,
                new HashMap<>(), interceptorBindings, annotationStore);

        this.injectionPoints = new CopyOnWriteArrayList<>();
        this.interceptors = new CopyOnWriteArrayList<>();
        this.decorators = new CopyOnWriteArrayList<>();
        this.beans = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();

        this.assignabilityCheck = new AssignabilityCheck(beanArchiveIndex, applicationIndex);
        this.beanResolver = new BeanResolverImpl(this);
        this.interceptorResolver = new InterceptorResolver(this);
        this.transformUnproxyableClasses = builder.transformUnproxyableClasses;
        this.jtaCapabilities = builder.jtaCapabilities;
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
                            beanDefiningAnnotations.add(new BeanDefiningAnnotation(scope.getDotName(), null));
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
        this.beans.addAll(findBeans(initBeanDefiningAnnotations(beanDefiningAnnotations, stereotypes.keySet()), observers,
                injectionPoints, jtaCapabilities));
        // Note that we need to use view of the collections to reflect further additions, e.g. synthetic beans and observers
        buildContextPut(Key.BEANS.asString(), Collections.unmodifiableList(beans));
        buildContextPut(Key.OBSERVERS.asString(), Collections.unmodifiableList(observers));

        this.interceptors.addAll(findInterceptors(injectionPoints));
        buildContextPut(Key.INTERCEPTORS.asString(), Collections.unmodifiableList(interceptors));
        this.decorators.addAll(findDecorators(injectionPoints));
        this.injectionPoints.addAll(injectionPoints);
        buildContextPut(Key.INJECTION_POINTS.asString(), Collections.unmodifiableList(this.injectionPoints));

        return registerSyntheticBeans(beanRegistrars, buildContext);
    }

    void init(Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            List<Predicate<BeanInfo>> additionalUnusedBeanExclusions) {
        long start = System.nanoTime();

        initBeanByTypeMap();
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
        }
        buildContext.putInternal(BuildExtension.Key.REMOVED_BEANS.asString(), Collections.unmodifiableSet(removedBeans));
        LOGGER.debugf("Bean deployment initialized in %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    private void initBeanByTypeMap() {
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
            List<InjectionPointInfo> removableInjectionPoints = removableInterceptors.stream()
                    .flatMap(d -> d.getAllInjectionPoints().stream()).collect(Collectors.toList());
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
            List<InjectionPointInfo> removableInjectionPoints = removableDecorators.stream()
                    .flatMap(d -> d.getAllInjectionPoints().stream()).collect(Collectors.toList());
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
            List<InjectionPointInfo> removableInjectionPoints = removableBeans.stream()
                    .flatMap(d -> d.getAllInjectionPoints().stream()).collect(Collectors.toList());
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
        validateBeans(errors, validators, bytecodeTransformerConsumer);
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

    /**
     * This index was used to discover components (beans, interceptors, qualifiers, etc.) and during type-safe resolution.
     * 
     * @return the bean archive index
     */
    public IndexView getBeanArchiveIndex() {
        return beanArchiveIndex;
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
        return (getQualifier(name).classAnnotation(DotNames.INHERITED) != null);
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
    Collection<AnnotationInstance> extractInterceptorBindings(AnnotationInstance annotation) {
        return extractAnnotations(annotation, interceptorBindings, repeatingInterceptorBindingAnnotations);
    }

    private static Collection<AnnotationInstance> extractAnnotations(AnnotationInstance annotation,
            Map<DotName, ClassInfo> singulars, Map<DotName, ClassInfo> repeatables) {
        DotName annotationName = annotation.name();
        if (singulars.get(annotationName) != null) {
            return Collections.singleton(annotation);
        } else {
            if (repeatables.get(annotationName) != null) {
                // repeatable, we need to extract actual annotations
                return new ArrayList<>(Arrays.asList(annotation.value().asNestedArray()));
            } else {
                // neither singular nor repeatable, return empty collection
                return Collections.emptyList();
            }
        }
    }

    BeanResolverImpl beanResolver() {
        return beanResolver;
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

    private void buildContextPut(String key, Object value) {
        if (buildContext != null) {
            buildContext.putInternal(key, value);
        }
    }

    private Map<DotName, ClassInfo> findQualifiers(IndexView index) {
        Map<DotName, ClassInfo> qualifiers = new HashMap<>();
        for (AnnotationInstance qualifier : index.getAnnotations(DotNames.QUALIFIER)) {
            ClassInfo qualifierClass = qualifier.target().asClass();
            if (isExcluded(qualifierClass)) {
                continue;
            }
            qualifiers.put(qualifierClass.name(), qualifierClass);
        }
        return qualifiers;
    }

    private Map<DotName, ClassInfo> findContainerAnnotations(Map<DotName, ClassInfo> annotations, IndexView index) {
        Map<DotName, ClassInfo> containerAnnotations = new HashMap<>();
        for (ClassInfo annotation : annotations.values()) {
            AnnotationInstance repeatableMetaAnnotation = annotation.classAnnotation(DotNames.REPEATABLE);
            if (repeatableMetaAnnotation != null) {
                DotName containerAnnotationName = repeatableMetaAnnotation.value().asClass().name();
                ClassInfo containerClass = getClassByName(index, containerAnnotationName);
                containerAnnotations.put(containerAnnotationName, containerClass);
            }
        }
        return containerAnnotations;
    }

    private Map<DotName, ClassInfo> findInterceptorBindings(IndexView index) {
        Map<DotName, ClassInfo> bindings = new HashMap<>();
        // Note: doesn't use AnnotationStore, this will operate on classes without applying annotation transformers
        for (AnnotationInstance binding : index.getAnnotations(DotNames.INTERCEPTOR_BINDING)) {
            ClassInfo bindingClass = binding.target().asClass();
            if (isExcluded(bindingClass)) {
                continue;
            }
            bindings.put(bindingClass.name(), bindingClass);
        }
        return bindings;
    }

    private static Map<DotName, Set<AnnotationInstance>> findTransitiveInterceptorBindings(Collection<DotName> initialBindings,
            IndexView index,
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

    private Map<DotName, StereotypeInfo> findStereotypes(IndexView index, Map<DotName, ClassInfo> interceptorBindings,
            Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations,
            Map<ScopeInfo, Function<MethodCreator, ResultHandle>> customContexts,
            Map<DotName, Collection<AnnotationInstance>> additionalStereotypes, AnnotationStore annotationStore) {

        Map<DotName, StereotypeInfo> stereotypes = new HashMap<>();
        final List<AnnotationInstance> stereotypeAnnotations = new ArrayList<>(index.getAnnotations(DotNames.STEREOTYPE));
        for (final Collection<AnnotationInstance> annotations : additionalStereotypes.values()) {
            stereotypeAnnotations.addAll(annotations);
        }
        Set<DotName> stereotypeNames = new HashSet<>();
        for (AnnotationInstance stereotype : stereotypeAnnotations) {
            stereotypeNames.add(stereotype.target().asClass().name());
        }
        for (AnnotationInstance stereotype : stereotypeAnnotations) {
            final DotName stereotypeName = stereotype.target().asClass().name();
            ClassInfo stereotypeClass = getClassByName(index, stereotypeName);
            if (stereotypeClass != null && !isExcluded(stereotypeClass)) {

                boolean isAlternative = false;
                Integer alternativePriority = null;
                List<ScopeInfo> scopes = new ArrayList<>();
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
                    } else {
                        final ScopeInfo scope = getScope(annotation.name(), customContexts);
                        if (scope != null) {
                            scopes.add(scope);
                        }
                    }
                }
                boolean isAdditionalStereotypeBuildItem = additionalStereotypes.containsKey(stereotypeName);
                final ScopeInfo scope = getValidScope(scopes, stereotypeClass);
                boolean isInherited = stereotypeClass.classAnnotation(DotNames.INHERITED) != null;
                stereotypes.put(stereotypeName, new StereotypeInfo(scope, bindings, isAlternative, alternativePriority,
                        isNamed, false, isAdditionalStereotypeBuildItem, stereotypeClass, isInherited, parentStereotypes));
            }
        }
        //if an additional bean defining annotation has a default scope we register it as a stereotype
        if (additionalBeanDefiningAnnotations != null) {
            for (BeanDefiningAnnotation i : additionalBeanDefiningAnnotations) {
                if (i.getDefaultScope() != null) {
                    ScopeInfo scope = getScope(i.getDefaultScope(), customContexts);
                    ClassInfo stereotypeClassInfo = getClassByName(index, i.getAnnotation());
                    boolean isInherited = stereotypeClassInfo.classAnnotation(DotNames.INHERITED) != null;
                    if (stereotypeClassInfo != null) {
                        stereotypes.put(i.getAnnotation(), new StereotypeInfo(scope, Collections.emptyList(),
                                false, null, false, true,
                                false, stereotypeClassInfo, isInherited, Collections.emptyList()));
                    }
                }
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

    static ScopeInfo getValidScope(Collection<ScopeInfo> stereotypeScopes, AnnotationTarget target) {
        switch (stereotypeScopes.size()) {
            case 0:
                return null;
            case 1:
                return stereotypeScopes.iterator().next();
            default:
                throw new DefinitionException("All stereotypes must specify the same scope or the bean must declare a scope: "
                        + target + " declares scopes " + stereotypeScopes.stream().map(ScopeInfo::getDotName)
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

        for (ClassInfo beanClass : beanArchiveIndex.getKnownClasses()) {

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

                // a bean without no-arg constructor needs to have either a constructor annotated with @Inject
                // or a single constructor
                if (numberOfConstructorsWithInject == 0 && numberOfConstructorsWithoutInject != 1) {
                    continue;
                }
            }

            if (annotationStore.hasAnnotation(beanClass, DotNames.VETOED)) {
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

            boolean hasBeanDefiningAnnotation = false;
            if (annotationStore.hasAnyAnnotation(beanClass, beanDefiningAnnotations)) {
                hasBeanDefiningAnnotation = true;
                beanClasses.add(beanClass);
            }

            // non-inherited stuff:
            for (MethodInfo method : beanClass.methods()) {
                if (Methods.isSynthetic(method)) {
                    continue;
                }
                if (annotationStore.getAnnotations(method).isEmpty()) {
                    continue;
                }
                if (annotationStore.hasAnnotation(method, DotNames.PRODUCES)
                        && !annotationStore.hasAnnotation(method, DotNames.VETOED_PRODUCER)) {
                    // Producers are not inherited
                    producerMethods.add(method);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Producer method found but %s has no bean defining annotation - using @Dependent",
                                beanClass);
                        beanClasses.add(beanClass);
                    }
                } else if (annotationStore.hasAnnotation(method, DotNames.DISPOSES)) {
                    // Disposers are not inherited
                    disposerMethods.add(method);
                }
            }

            // inherited stuff
            ClassInfo aClass = beanClass;
            Set<Methods.MethodKey> methods = new HashSet<>();
            while (aClass != null) {
                for (MethodInfo method : aClass.methods()) {
                    Methods.MethodKey methodDescriptor = new Methods.MethodKey(method);
                    if (Methods.isSynthetic(method) || Methods.isOverriden(methodDescriptor, methods)) {
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
                        if (!Modifier.isAbstract(beanClass.flags())) {
                            // add only concrete classes
                            beanClasses.add(beanClass);
                            if (!hasBeanDefiningAnnotation) {
                                LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent",
                                        beanClass);
                            }
                        }
                    } else if (annotationStore.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                        asyncObserverMethods.computeIfAbsent(method, ignored -> new HashSet<>())
                                .add(beanClass);
                        if (!Modifier.isAbstract(beanClass.flags())) {
                            // add only concrete classes
                            beanClasses.add(beanClass);
                            if (!hasBeanDefiningAnnotation) {
                                LOGGER.debugf("Observer method found but %s has no bean defining annotation - using @Dependent",
                                        beanClass);
                            }
                        }
                    }
                }
                DotName superType = aClass.superName();
                aClass = superType != null && !superType.equals(DotNames.OBJECT)
                        ? getClassByName(beanArchiveIndex, superType)
                        : null;
            }
            for (FieldInfo field : beanClass.fields()) {
                if (annotationStore.hasAnnotation(field, DotNames.PRODUCES)
                        && !annotationStore.hasAnnotation(field, DotNames.VETOED_PRODUCER)) {
                    if (annotationStore.hasAnnotation(field, DotNames.INJECT)) {
                        throw new DefinitionException("Injected field cannot be annotated with @Produces: " + field);
                    }
                    // Producer fields are not inherited
                    producerFields.add(field);
                    if (!hasBeanDefiningAnnotation) {
                        LOGGER.debugf("Producer field found but %s has no bean defining annotation - using @Dependent",
                                beanClass);
                        beanClasses.add(beanClass);
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
            }
        }

        List<DisposerInfo> disposers = new ArrayList<>();
        for (MethodInfo disposerMethod : disposerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(disposerMethod.declaringClass());
            if (declaringBean != null) {
                Injection injection = Injection.forDisposer(disposerMethod, declaringBean.getImplClazz(), this,
                        injectionPointTransformer);
                disposers.add(new DisposerInfo(declaringBean, disposerMethod, injection));
                injectionPoints.addAll(injection.injectionPoints);
            }
        }

        for (MethodInfo producerMethod : producerMethods) {
            BeanInfo declaringBean = beanClassToBean.get(producerMethod.declaringClass());
            if (declaringBean != null) {
                BeanInfo producerMethodBean = Beans.createProducerMethod(producerMethod, declaringBean, this,
                        findDisposer(declaringBean, producerMethod, disposers), injectionPointTransformer);
                if (producerMethodBean != null) {
                    beans.add(producerMethodBean);
                    injectionPoints.addAll(producerMethodBean.getAllInjectionPoints());
                }
            }
        }

        for (FieldInfo producerField : producerFields) {
            BeanInfo declaringBean = beanClassToBean.get(producerField.declaringClass());
            if (declaringBean != null) {
                BeanInfo producerFieldBean = Beans.createProducerField(producerField, declaringBean, this,
                        findDisposer(declaringBean, producerField, disposers));
                if (producerFieldBean != null) {
                    beans.add(producerFieldBean);
                }
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
                ObserverInfo observer = ObserverInfo.create(declaringBean, observerMethod, injection, async,
                        observerTransformers, buildContext, jtaCapabilities);
                if (observer != null) {
                    observers.add(observer);
                    injectionPoints.addAll(injection.injectionPoints);
                }
            }
        }
    }

    private DisposerInfo findDisposer(BeanInfo declaringBean, AnnotationTarget annotationTarget, List<DisposerInfo> disposers) {
        List<DisposerInfo> found = new ArrayList<>();
        Type beanType;
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        List<AnnotationInstance> allAnnotations;
        if (Kind.FIELD.equals(annotationTarget.kind())) {
            allAnnotations = annotationTarget.asField().annotations();
            beanType = annotationTarget.asField().type();
        } else if (Kind.METHOD.equals(annotationTarget.kind())) {
            allAnnotations = annotationTarget.asMethod().annotations();
            beanType = annotationTarget.asMethod().returnType();
        } else {
            throw new RuntimeException("Unsupported annotation target: " + annotationTarget);
        }
        allAnnotations.forEach(a -> extractQualifiers(a).forEach(qualifiers::add));
        for (DisposerInfo disposer : disposers) {
            if (disposer.getDeclaringBean().equals(declaringBean)) {
                boolean hasQualifier = true;
                for (AnnotationInstance disposerQualifier : disposer.getDisposedParameterQualifiers()) {
                    if (!Beans.hasQualifier(declaringBean.getDeployment(), disposerQualifier, qualifiers)) {
                        hasQualifier = false;
                    }
                }
                if (hasQualifier && beanResolver.matches(disposer.getDisposedParameterType(), beanType)) {
                    found.add(disposer);
                }
            }
        }
        if (found.size() > 1) {
            throw new DefinitionException("Multiple disposer methods found for " + annotationTarget);
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
                jtaCapabilities, configurator.notifyConsumer));
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
        for (AnnotationInstance annotation : beanArchiveIndex.getAnnotations(DotNames.INTERCEPTOR)) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                interceptorClasses.put(annotation.target().asClass().name(), annotation.target().asClass());
            }
        }
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (ClassInfo interceptorClass : interceptorClasses.values()) {
            if (annotationStore.hasAnnotation(interceptorClass, DotNames.VETOED) || isExcluded(interceptorClass)) {
                // Skip vetoed interceptors
                continue;
            }
            interceptors
                    .add(Interceptors.createInterceptor(interceptorClass, this, injectionPointTransformer, annotationStore));
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
        for (AnnotationInstance annotation : beanArchiveIndex.getAnnotations(DotNames.DECORATOR)) {
            if (Kind.CLASS.equals(annotation.target().kind())) {
                decoratorClasses.put(annotation.target().asClass().name(), annotation.target().asClass());
            }
        }
        List<DecoratorInfo> decorators = new ArrayList<>();
        for (ClassInfo decoratorClass : decoratorClasses.values()) {
            if (annotationStore.hasAnnotation(decoratorClass, DotNames.VETOED) || isExcluded(decoratorClass)) {
                // Skip vetoed decorators
                continue;
            }
            decorators
                    .add(Decorators.createDecorator(decoratorClass, this, injectionPointTransformer, annotationStore));
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

    private void validateBeans(List<Throwable> errors, List<BeanDeploymentValidator> validators,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {

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
            }
            bean.validate(errors, validators, bytecodeTransformerConsumer, classesReceivingNoArgsCtor);
        }

        if (!namedBeans.isEmpty()) {
            for (Entry<String, List<BeanInfo>> entry : namedBeans.entrySet()) {
                if (entry.getValue()
                        .size() > 1) {
                    if (Beans.resolveAmbiguity(entry.getValue()) == null) {
                        errors.add(new DeploymentException("Unresolvable ambiguous bean name detected: " + entry.getKey()
                                + "\nBeans:\n" + entry.getValue()
                                        .stream()
                                        .map(Object::toString)
                                        .collect(Collectors.joining("\n"))));
                    }
                }
            }
        }
    }

    public Set<String> getInterceptorNonbindingMembers(DotName name) {
        return interceptorNonbindingMembers.getOrDefault(name, Collections.emptySet());
    }

    public Set<String> getQualifierNonbindingMembers(DotName name) {
        return qualifierNonbindingMembers.getOrDefault(name, Collections.emptySet());
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

    private static class BeanRegistrationContextImpl extends RegistrationContextImpl implements RegistrationContext {

        BeanRegistrationContextImpl(BuildContext buildContext, BeanDeployment beanDeployment) {
            super(buildContext, beanDeployment);
        }

        @Override
        public <T> BeanConfigurator<T> configure(DotName beanClassName) {
            return new BeanConfigurator<T>(beanClassName, beanDeployment, beanDeployment::addSyntheticBean);
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
