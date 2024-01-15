package io.quarkus.arc.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext;
import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.arc.processor.CustomAlterableContexts.CustomAlterableContextInfo;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.arc.processor.bcextensions.ExtensionsEntryPoint;
import io.quarkus.gizmo.BytecodeCreator;

/**
 * An integrator should create a new instance of the bean processor using the convenient {@link Builder} and then invoke the
 * "processing" methods in the following order:
 *
 * <ol>
 * <li>{@link #registerCustomContexts()}</li>
 * <li>{@link #registerScopes()}</li>
 * <li>{@link #registerBeans()}</li>
 * <li>{@link #initialize(Consumer)}</li>
 * <li>{@link #validate(Consumer)}</li>
 * <li>{@link #processValidationErrors(io.quarkus.arc.processor.BeanDeploymentValidator.ValidationContext)}</li>
 * <li>{@link #generateResources(ReflectionRegistration, Set, Consumer)}</li>
 * </ol>
 */
public class BeanProcessor {

    public static Builder builder() {
        return new Builder();
    }

    static final String DEFAULT_NAME = "Default";

    static final Logger LOGGER = Logger.getLogger(BeanProcessor.class);

    private final String name;
    private final ResourceOutput output;
    private final AnnotationLiteralProcessor annotationLiterals;
    private final ReflectionRegistration reflectionRegistration;
    private final List<BeanRegistrar> beanRegistrars;
    private final List<ContextRegistrar> contextRegistrars;
    private final List<ObserverRegistrar> observerRegistrars;
    private final List<BeanDeploymentValidator> beanDeploymentValidators;
    private final BuildContextImpl buildContext;
    private final Predicate<DotName> applicationClassPredicate;
    private final BeanDeployment beanDeployment;
    private final boolean generateSources;
    private final boolean allowMocking;
    private final boolean transformUnproxyableClasses;
    private final Predicate<BeanDeployment> optimizeContexts;
    private final List<Function<BeanInfo, Consumer<BytecodeCreator>>> suppressConditionGenerators;

    // This predicate is used to filter annotations for InjectionPoint metadata
    // Note that we do create annotation literals for all annotations for an injection point that resolves to a @Dependent bean that injects the InjectionPoint metadata
    // The original use case is to ignore JDK annotations that would prevent an application built with JDK 9+ from targeting JDK 8
    // Such as java.lang.Deprecated
    protected final Predicate<DotName> injectionPointAnnotationsPredicate;

    private final ExtensionsEntryPoint buildCompatibleExtensions;
    private final CustomAlterableContexts customAlterableContexts; // generic but currently only used for BCE

    private BeanProcessor(Builder builder) {
        this.buildCompatibleExtensions = builder.buildCompatibleExtensions;
        this.customAlterableContexts = new CustomAlterableContexts(builder.applicationClassPredicate);
        if (buildCompatibleExtensions != null) {
            buildCompatibleExtensions.registerMetaAnnotations(builder, customAlterableContexts);
            buildCompatibleExtensions.runEnhancement(builder.beanArchiveComputingIndex, builder);
        }

        this.reflectionRegistration = builder.reflectionRegistration;
        this.applicationClassPredicate = builder.applicationClassPredicate;
        this.name = builder.name;
        this.output = builder.output;
        this.annotationLiterals = new AnnotationLiteralProcessor(
                builder.beanArchiveComputingIndex != null ? builder.beanArchiveComputingIndex
                        : builder.beanArchiveImmutableIndex,
                applicationClassPredicate);
        this.generateSources = builder.generateSources;
        this.allowMocking = builder.allowMocking;
        this.optimizeContexts = builder.optimizeContexts;
        this.transformUnproxyableClasses = builder.transformUnproxyableClasses;
        this.suppressConditionGenerators = builder.suppressConditionGenerators;

        // Initialize all build processors
        buildContext = new BuildContextImpl();
        buildContext.putInternal(Key.INDEX, builder.beanArchiveComputingIndex != null ? builder.beanArchiveComputingIndex
                : builder.beanArchiveImmutableIndex);

        this.beanRegistrars = initAndSort(builder.beanRegistrars, buildContext);
        this.observerRegistrars = initAndSort(builder.observerRegistrars, buildContext);
        this.contextRegistrars = initAndSort(builder.contextRegistrars, buildContext);
        this.beanDeploymentValidators = initAndSort(builder.beanDeploymentValidators, buildContext);
        this.beanDeployment = new BeanDeployment(name, buildContext, builder);
        buildContext.putInternal(Key.DEPLOYMENT, this.beanDeployment);

        // Make it configurable if we find that the set of annotations needs to grow
        this.injectionPointAnnotationsPredicate = Predicate.not(DotNames.DEPRECATED::equals);
    }

    public ContextRegistrar.RegistrationContext registerCustomContexts() {
        return beanDeployment.registerCustomContexts(contextRegistrars);
    }

    public void registerScopes() {
        beanDeployment.registerScopes();
    }

    /**
     * Analyze the deployment and register all beans and observers declared on the classes. Furthermore, register all synthetic
     * beans provided by bean registrars.
     *
     * @return the context applied to {@link BeanRegistrar}
     */
    public BeanRegistrar.RegistrationContext registerBeans() {
        return beanDeployment.registerBeans(beanRegistrars);
    }

    public ObserverRegistrar.RegistrationContext registerSyntheticObservers() {
        return beanDeployment.registerSyntheticObservers(observerRegistrars);
    }

    /**
     *
     * @param bytecodeTransformerConsumer Used to register a bytecode transformation
     * @param additionalUnusedBeanExclusions Additional predicates to exclude unused beans
     */
    public void initialize(Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            List<Predicate<BeanInfo>> additionalUnusedBeanExclusions) {
        beanDeployment.init(bytecodeTransformerConsumer, additionalUnusedBeanExclusions);
    }

    /**
     *
     * @param bytecodeTransformerConsumer Used to register a bytecode transformation
     * @return the validation context
     */
    public BeanDeploymentValidator.ValidationContext validate(Consumer<BytecodeTransformer> bytecodeTransformerConsumer) {
        ValidationContext validationContext = beanDeployment.validate(beanDeploymentValidators, bytecodeTransformerConsumer);
        customAlterableContexts.validate(validationContext, transformUnproxyableClasses, bytecodeTransformerConsumer);
        if (buildCompatibleExtensions != null) {
            buildCompatibleExtensions.runValidation(beanDeployment.getBeanArchiveIndex(),
                    validationContext.get(Key.BEANS), validationContext.get(Key.OBSERVERS));
            buildCompatibleExtensions.registerValidationErrors(validationContext);
        }
        return validationContext;
    }

    public void processValidationErrors(BeanDeploymentValidator.ValidationContext validationContext) {
        BeanDeployment.processErrors(validationContext.getDeploymentProblems());
    }

    public List<Resource> generateResources(ReflectionRegistration reflectionRegistration, Set<String> existingClasses,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean detectUnusedFalsePositives,
            ExecutorService executor)
            throws IOException, InterruptedException, ExecutionException {

        ReflectionRegistration refReg = reflectionRegistration != null ? reflectionRegistration : this.reflectionRegistration;
        PrivateMembersCollector privateMembers = new PrivateMembersCollector();
        boolean optimizeContextsValue = optimizeContexts != null ? optimizeContexts.test(beanDeployment) : false;

        // These maps are precomputed and then used in the ComponentsProviderGenerator which is generated first
        Map<BeanInfo, String> beanToGeneratedName = new HashMap<>();
        Map<ObserverInfo, String> observerToGeneratedName = new HashMap<>();
        Map<DotName, String> scopeToGeneratedName = new HashMap<>();

        BeanGenerator beanGenerator = new BeanGenerator(annotationLiterals, applicationClassPredicate, privateMembers,
                generateSources, refReg, existingClasses, beanToGeneratedName,
                injectionPointAnnotationsPredicate, suppressConditionGenerators);
        Collection<BeanInfo> beans = beanDeployment.getBeans();
        for (BeanInfo bean : beans) {
            beanGenerator.precomputeGeneratedName(bean);
        }

        CustomAlterableContextsGenerator alterableContextsGenerator = new CustomAlterableContextsGenerator(generateSources);
        List<CustomAlterableContextInfo> alterableContexts = customAlterableContexts.getRegistered();

        // Set of normal scopes for which the client proxy delegate can be optimized
        Set<DotName> singleContextNormalScopes = findSingleContextNormalScopes();

        ClientProxyGenerator clientProxyGenerator = new ClientProxyGenerator(applicationClassPredicate, generateSources,
                allowMocking, refReg, existingClasses, singleContextNormalScopes);

        InterceptorGenerator interceptorGenerator = new InterceptorGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers, generateSources, refReg, existingClasses, beanToGeneratedName,
                injectionPointAnnotationsPredicate);
        Collection<InterceptorInfo> interceptors = beanDeployment.getInterceptors();
        for (InterceptorInfo interceptor : interceptors) {
            interceptorGenerator.precomputeGeneratedName(interceptor);
        }
        interceptors.forEach(interceptorGenerator::precomputeGeneratedName);

        DecoratorGenerator decoratorGenerator = new DecoratorGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers, generateSources, refReg, existingClasses, beanToGeneratedName,
                injectionPointAnnotationsPredicate);
        Collection<DecoratorInfo> decorators = beanDeployment.getDecorators();
        for (DecoratorInfo decorator : decorators) {
            decoratorGenerator.precomputeGeneratedName(decorator);
        }

        SubclassGenerator subclassGenerator = new SubclassGenerator(annotationLiterals, applicationClassPredicate,
                generateSources, refReg, existingClasses, privateMembers);

        ObserverGenerator observerGenerator = new ObserverGenerator(annotationLiterals, applicationClassPredicate,
                privateMembers, generateSources, refReg, existingClasses, observerToGeneratedName,
                injectionPointAnnotationsPredicate, allowMocking);
        Collection<ObserverInfo> observers = beanDeployment.getObservers();
        for (ObserverInfo observer : observers) {
            observerGenerator.precomputeGeneratedName(observer);
        }

        ContextInstancesGenerator contextInstancesGenerator = new ContextInstancesGenerator(generateSources,
                refReg, beanDeployment, scopeToGeneratedName);
        if (optimizeContextsValue) {
            contextInstancesGenerator.precomputeGeneratedName(BuiltinScope.APPLICATION.getName());
            contextInstancesGenerator.precomputeGeneratedName(BuiltinScope.REQUEST.getName());
        }

        List<Resource> resources = new ArrayList<>();

        if (executor != null) {
            LOGGER.debug("Generating resources in parallel");

            // Primary tasks include interceptors, decorators, beans and observers
            List<Future<Collection<Resource>>> primaryTasks = new ArrayList<>();
            // Secondary tasks include client proxies and subclasses - this queue is accessed concurrently
            ConcurrentLinkedQueue<Future<Collection<Resource>>> secondaryTasks = new ConcurrentLinkedQueue<>();

            // Generate _ComponentsProvider
            primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                @Override
                public Collection<Resource> call() throws Exception {
                    return new ComponentsProviderGenerator(annotationLiterals, generateSources, detectUnusedFalsePositives)
                            .generate(
                                    name,
                                    beanDeployment,
                                    beanToGeneratedName,
                                    observerToGeneratedName,
                                    scopeToGeneratedName);
                }
            }));

            // Generate interceptors
            for (InterceptorInfo interceptor : interceptors) {
                primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                    @Override
                    public Collection<Resource> call() throws Exception {
                        return interceptorGenerator.generate(interceptor);
                    }
                }));
            }
            // Generate decorators
            for (DecoratorInfo decorator : decorators) {
                primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                    @Override
                    public Collection<Resource> call() throws Exception {
                        return decoratorGenerator.generate(decorator);
                    }
                }));
            }
            // Generate beans
            for (BeanInfo bean : beans) {

                primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                    @Override
                    public Collection<Resource> call() throws Exception {

                        Collection<Resource> beanResources = beanGenerator.generate(bean);
                        for (Resource resource : beanResources) {
                            if (SpecialType.BEAN == resource.getSpecialType()) {

                                if (bean.getScope().isNormal()) {
                                    // Generate client proxy
                                    secondaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                                        @Override
                                        public Collection<Resource> call() throws Exception {
                                            Collection<Resource> proxyResources = clientProxyGenerator.generate(bean,
                                                    resource.getFullyQualifiedName(),
                                                    bytecodeTransformerConsumer, transformUnproxyableClasses);
                                            if (bean.isClassBean()) {
                                                for (Resource r : proxyResources) {
                                                    if (r.getSpecialType() == SpecialType.CLIENT_PROXY) {
                                                        refReg.registerClientProxy(bean.getBeanClass(),
                                                                r.getFullyQualifiedName());
                                                        break;
                                                    }
                                                }
                                            }
                                            return proxyResources;
                                        }
                                    }));
                                }

                                if (bean.isSubclassRequired()) {
                                    // Generate subclass
                                    secondaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                                        @Override
                                        public Collection<Resource> call() throws Exception {
                                            Collection<Resource> subclassResources = subclassGenerator.generate(bean,
                                                    resource.getFullyQualifiedName());
                                            for (Resource r : subclassResources) {
                                                if (r.getSpecialType() == SpecialType.SUBCLASS) {
                                                    refReg.registerSubclass(bean.getBeanClass(), r.getFullyQualifiedName());
                                                    break;
                                                }
                                            }
                                            return subclassResources;
                                        }
                                    }));
                                }
                            }
                        }
                        return beanResources;
                    }
                }));
            }

            // Generate observers
            for (ObserverInfo observer : observers) {
                primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                    @Override
                    public Collection<Resource> call() throws Exception {
                        return observerGenerator.generate(observer);
                    }
                }));
            }

            // Generate `_InjectableContext` subclasses for custom `AlterableContext`s
            for (CustomAlterableContextInfo info : alterableContexts) {
                primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {
                    @Override
                    public Collection<Resource> call() throws Exception {
                        return alterableContextsGenerator.generate(info);
                    }
                }));
            }

            if (optimizeContextsValue) {
                // Generate _ContextInstances
                primaryTasks.add(executor.submit(new Callable<Collection<Resource>>() {

                    @Override
                    public Collection<Resource> call() throws Exception {
                        Collection<Resource> resources = new ArrayList<>();
                        resources.addAll(contextInstancesGenerator.generate(BuiltinScope.APPLICATION.getName()));
                        resources.addAll(contextInstancesGenerator.generate(BuiltinScope.REQUEST.getName()));
                        return resources;
                    }
                }));
            }

            for (Future<Collection<Resource>> future : primaryTasks) {
                resources.addAll(future.get());
            }
            for (Future<Collection<Resource>> future : secondaryTasks) {
                resources.addAll(future.get());
            }

        } else {
            LOGGER.debug("Generating resources in series");

            // Generate interceptors
            for (InterceptorInfo interceptor : interceptors) {
                resources.addAll(interceptorGenerator.generate(interceptor));
            }
            // Generate decorators
            for (DecoratorInfo decorator : decorators) {
                resources.addAll(decoratorGenerator.generate(decorator));
            }
            // Generate beans
            for (BeanInfo bean : beans) {
                for (Resource resource : beanGenerator.generate(bean)) {
                    resources.add(resource);
                    if (SpecialType.BEAN.equals(resource.getSpecialType())) {
                        if (bean.getScope().isNormal()) {
                            // Generate client proxy
                            Collection<Resource> proxyResources = clientProxyGenerator.generate(bean,
                                    resource.getFullyQualifiedName(),
                                    bytecodeTransformerConsumer, transformUnproxyableClasses);
                            if (bean.isClassBean()) {
                                for (Resource r : proxyResources) {
                                    if (r.getSpecialType() == SpecialType.CLIENT_PROXY) {
                                        refReg.registerClientProxy(bean.getBeanClass(),
                                                r.getFullyQualifiedName());
                                        break;
                                    }
                                }
                            }
                            resources.addAll(proxyResources);
                        }
                        if (bean.isSubclassRequired()) {
                            Collection<Resource> subclassResources = subclassGenerator.generate(bean,
                                    resource.getFullyQualifiedName());
                            for (Resource r : subclassResources) {
                                if (r.getSpecialType() == SpecialType.SUBCLASS) {
                                    refReg.registerSubclass(bean.getBeanClass(), r.getFullyQualifiedName());
                                    break;
                                }
                            }
                            resources.addAll(subclassResources);
                        }
                    }
                }
            }
            // Generate observers
            for (ObserverInfo observer : observers) {
                resources.addAll(observerGenerator.generate(observer));
            }

            // Generate `_InjectableContext` subclasses for custom `AlterableContext`s
            for (CustomAlterableContextInfo info : alterableContexts) {
                resources.addAll(alterableContextsGenerator.generate(info));
            }

            // Generate _ComponentsProvider
            resources.addAll(
                    new ComponentsProviderGenerator(annotationLiterals, generateSources, detectUnusedFalsePositives).generate(
                            name,
                            beanDeployment,
                            beanToGeneratedName,
                            observerToGeneratedName,
                            scopeToGeneratedName));

            if (optimizeContextsValue) {
                // Generate _ContextInstances
                resources.addAll(contextInstancesGenerator.generate(BuiltinScope.APPLICATION.getName()));
                resources.addAll(contextInstancesGenerator.generate(BuiltinScope.REQUEST.getName()));
            }
        }

        // Generate AnnotationLiterals - at this point all annotation literals must be processed
        if (annotationLiterals.hasLiteralsToGenerate()) {
            AnnotationLiteralGenerator generator = new AnnotationLiteralGenerator(generateSources);
            if (executor != null) {
                Collection<Future<Collection<Resource>>> annotationTasks = generator.generate(annotationLiterals.getCache(),
                        existingClasses, executor);
                for (Future<Collection<Resource>> future : annotationTasks) {
                    resources.addAll(future.get());
                }
            } else {
                resources.addAll(generator.generate(annotationLiterals.getCache(), existingClasses));
            }
        }

        privateMembers.log();

        if (output != null) {
            for (Resource resource : resources) {
                output.writeResource(resource);
            }
        }
        return resources;
    }

    public BeanDeployment getBeanDeployment() {
        return beanDeployment;
    }

    public AnnotationLiteralProcessor getAnnotationLiteralProcessor() {
        return annotationLiterals;
    }

    public BeanDeployment process() throws IOException, InterruptedException, ExecutionException {
        Consumer<BytecodeTransformer> unsupportedBytecodeTransformer = new Consumer<BytecodeTransformer>() {
            @Override
            public void accept(BytecodeTransformer transformer) {
                throw new UnsupportedOperationException();
            }
        };
        registerCustomContexts();
        registerScopes();
        registerBeans();
        beanDeployment.initBeanByTypeMap();
        registerSyntheticObservers();
        initialize(unsupportedBytecodeTransformer, Collections.emptyList());
        ValidationContext validationContext = validate(unsupportedBytecodeTransformer);
        processValidationErrors(validationContext);
        generateResources(null, new HashSet<>(), unsupportedBytecodeTransformer, beanDeployment.removeUnusedBeans, null);
        return beanDeployment;
    }

    public Predicate<DotName> getInjectionPointAnnotationsPredicate() {
        return injectionPointAnnotationsPredicate;
    }

    private Set<DotName> findSingleContextNormalScopes() {
        Map<DotName, Integer> contextsForScope = new HashMap<>();
        // built-in contexts
        contextsForScope.put(BuiltinScope.REQUEST.getName(), 1);
        // custom contexts
        beanDeployment.getCustomContexts()
                .keySet()
                .stream()
                .filter(ScopeInfo::isNormal)
                .map(ScopeInfo::getDotName)
                .forEach(scope -> contextsForScope.merge(scope, 1, Integer::sum));

        return contextsForScope.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static class Builder {

        String name;
        IndexView beanArchiveComputingIndex;
        IndexView beanArchiveImmutableIndex;
        IndexView applicationIndex;
        Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations;
        ResourceOutput output;
        ReflectionRegistration reflectionRegistration;

        final List<DotName> resourceAnnotations;
        final List<AnnotationsTransformer> annotationTransformers;
        final List<InjectionPointsTransformer> injectionPointTransformers;
        final List<ObserverTransformer> observerTransformers;
        final List<BeanRegistrar> beanRegistrars;
        final List<ObserverRegistrar> observerRegistrars;
        final List<ContextRegistrar> contextRegistrars;
        final List<QualifierRegistrar> qualifierRegistrars;
        final List<InterceptorBindingRegistrar> interceptorBindingRegistrars;
        final List<StereotypeRegistrar> stereotypeRegistrars;
        final List<BeanDeploymentValidator> beanDeploymentValidators;
        final List<Function<BeanInfo, Consumer<BytecodeCreator>>> suppressConditionGenerators;

        boolean removeUnusedBeans = false;
        final List<Predicate<BeanInfo>> removalExclusions;

        boolean generateSources;
        boolean jtaCapabilities;
        boolean transformUnproxyableClasses;
        boolean transformPrivateInjectedFields;
        boolean failOnInterceptedPrivateMethod;
        boolean allowMocking;
        boolean strictCompatibility;
        Predicate<BeanDeployment> optimizeContexts;

        AlternativePriorities alternativePriorities;
        final List<Predicate<ClassInfo>> excludeTypes;

        ExtensionsEntryPoint buildCompatibleExtensions;

        Predicate<DotName> applicationClassPredicate;

        public Builder() {
            name = DEFAULT_NAME;
            additionalBeanDefiningAnnotations = Collections.emptySet();
            reflectionRegistration = ReflectionRegistration.NOOP;
            resourceAnnotations = new ArrayList<>();
            annotationTransformers = new ArrayList<>();
            injectionPointTransformers = new ArrayList<>();
            observerTransformers = new ArrayList<>();
            beanRegistrars = new ArrayList<>();
            observerRegistrars = new ArrayList<>();
            contextRegistrars = new ArrayList<>();
            qualifierRegistrars = new ArrayList<>();
            interceptorBindingRegistrars = new ArrayList<>();
            stereotypeRegistrars = new ArrayList<>();
            beanDeploymentValidators = new ArrayList<>();
            suppressConditionGenerators = new ArrayList<>();

            removeUnusedBeans = false;
            removalExclusions = new ArrayList<>();

            generateSources = false;
            jtaCapabilities = false;
            transformUnproxyableClasses = false;
            transformPrivateInjectedFields = false;
            failOnInterceptedPrivateMethod = false;
            allowMocking = false;
            strictCompatibility = false;

            excludeTypes = new ArrayList<>();

            applicationClassPredicate = dn -> true;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the computing bean archive index. This index is optional and can be used for example during type-safe resolution.
         * If it's not set then the immutable index is used instead.
         * <p>
         * The computing index must be built on top of the immutable index and compute only the classes that are not part of the
         * immutable index.
         * <p>
         * This index is never used to discover components (beans, observers, etc.).
         *
         * @param index
         * @return self
         * @see Builder#setImmutableBeanArchiveIndex(IndexView)
         */
        public Builder setComputingBeanArchiveIndex(IndexView index) {
            this.beanArchiveComputingIndex = index;
            return this;
        }

        /**
         * Set the immutable bean archive index. This index is mandatory and is used to discover components (beans, observers,
         * etc.).
         *
         * @param index
         * @return self
         * @see Builder#setComputingBeanArchiveIndex(IndexView)
         */
        public Builder setImmutableBeanArchiveIndex(IndexView index) {
            this.beanArchiveImmutableIndex = index;
            return this;
        }

        /**
         * Set the application index. This index is optional and is also used to discover types during type-safe resolution.
         * <p>
         * Some types may not be part of the bean archive index but are still needed during type-safe resolution.
         *
         * @param index
         * @return self
         */
        public Builder setApplicationIndex(IndexView index) {
            this.applicationIndex = index;
            return this;
        }

        public Builder setAdditionalBeanDefiningAnnotations(
                Collection<BeanDefiningAnnotation> additionalBeanDefiningAnnotations) {
            Objects.requireNonNull(additionalBeanDefiningAnnotations);
            this.additionalBeanDefiningAnnotations = additionalBeanDefiningAnnotations;
            return this;
        }

        public Builder addQualifierRegistrar(QualifierRegistrar qualifierRegistrar) {
            this.qualifierRegistrars.add(qualifierRegistrar);
            return this;
        }

        public Builder addInterceptorBindingRegistrar(InterceptorBindingRegistrar bindingRegistrar) {
            this.interceptorBindingRegistrars.add(bindingRegistrar);
            return this;
        }

        public Builder addStereotypeRegistrar(StereotypeRegistrar stereotypeRegistrar) {
            this.stereotypeRegistrars.add(stereotypeRegistrar);
            return this;
        }

        public Builder setOutput(ResourceOutput output) {
            this.output = output;
            return this;
        }

        public Builder setReflectionRegistration(ReflectionRegistration reflectionRegistration) {
            this.reflectionRegistration = reflectionRegistration;
            return this;
        }

        public Builder addAnnotationTransformer(AnnotationsTransformer transformer) {
            this.annotationTransformers.add(transformer);
            return this;
        }

        public Builder addInjectionPointTransformer(InjectionPointsTransformer transformer) {
            this.injectionPointTransformers.add(transformer);
            return this;
        }

        public Builder addObserverTransformer(ObserverTransformer transformer) {
            this.observerTransformers.add(transformer);
            return this;
        }

        public Builder addResourceAnnotations(Collection<DotName> resourceAnnotations) {
            this.resourceAnnotations.addAll(resourceAnnotations);
            return this;
        }

        public Builder addBeanRegistrar(BeanRegistrar registrar) {
            this.beanRegistrars.add(registrar);
            return this;
        }

        public Builder addObserverRegistrar(ObserverRegistrar registrar) {
            this.observerRegistrars.add(registrar);
            return this;
        }

        public Builder addContextRegistrar(ContextRegistrar registrar) {
            this.contextRegistrars.add(registrar);
            return this;
        }

        public Builder addBeanDeploymentValidator(BeanDeploymentValidator validator) {
            this.beanDeploymentValidators.add(validator);
            return this;
        }

        public Builder setApplicationClassPredicate(Predicate<DotName> applicationClassPredicate) {
            this.applicationClassPredicate = applicationClassPredicate;
            return this;
        }

        public Builder setJtaCapabilities(boolean jtaCapabilities) {
            this.jtaCapabilities = jtaCapabilities;
            return this;
        }

        public void setAllowMocking(boolean allowMocking) {
            this.allowMocking = allowMocking;
        }

        /**
         * If set to true the container will attempt to remove all unused beans.
         * <p>
         * An unused bean:
         * <ul>
         * <li>is not a built-in bean or interceptor,</li>
         * <li>is not eligible for injection to any injection point,</li>
         * <li>is not excluded - see {@link #addRemovalExclusion(Predicate)},</li>
         * <li>does not have a name,</li>
         * <li>does not declare an observer,</li>
         * <li>does not declare any producer which is eligible for injection to any injection point,</li>
         * <li>is not directly eligible for injection into any {@link jakarta.enterprise.inject.Instance} injection point</li>
         * </ul>
         *
         * @param removeUnusedBeans
         * @return self
         */
        public Builder setRemoveUnusedBeans(boolean removeUnusedBeans) {
            this.removeUnusedBeans = removeUnusedBeans;
            return this;
        }

        /**
         * Exclude unused beans that match the given predicate from removal.
         *
         * @param predicate
         * @return self
         * @see #setRemoveUnusedBeans(boolean)
         */
        public Builder addRemovalExclusion(Predicate<BeanInfo> predicate) {
            this.removalExclusions.add(predicate);
            return this;
        }

        /**
         * If set to true the container will transform unproxyable bean classes during validation.
         *
         * @param value
         * @return self
         */
        public Builder setTransformUnproxyableClasses(boolean value) {
            this.transformUnproxyableClasses = value;
            return this;
        }

        /**
         * If set to true the container will transform injected private field of class based beans during validation.
         *
         * @param value
         * @return self
         */
        public Builder setTransformPrivateInjectedFields(boolean value) {
            this.transformPrivateInjectedFields = value;
            return this;
        }

        /**
         * If set to true, the build will fail if an annotation that would result in an interceptor being created (such as
         * {@code @Transactional})
         */
        public void setFailOnInterceptedPrivateMethod(boolean failOnInterceptedPrivateMethod) {
            this.failOnInterceptedPrivateMethod = failOnInterceptedPrivateMethod;
        }

        /**
         * If set to true the will generate source files of all generated classes for debug purposes. The generated source is
         * not actually a source file but a textual representation of generated code.
         *
         * @param value
         * @return self
         */
        public Builder setGenerateSources(boolean value) {
            this.generateSources = value;
            return this;
        }

        /**
         * If set to {@code true}, the container will perform additional validations mandated by the CDI specification.
         * Some improvements on top of the CDI specification may be disabled. Applications that work as expected
         * in the strict mode should work without a change in the default, non-strict mode.
         * <p>
         * The strict mode is mainly introduced to allow passing the CDI Lite TCK. Applications are recommended
         * to use the default, non-strict mode, which makes CDI more convenient to use. The "strictness" of
         * the strict mode (the set of additional validations and the set of disabled improvements on top of
         * the CDI specification) may change over time.
         * <p>
         * Note that {@link #setTransformUnproxyableClasses(boolean)} also has effect on specification compatibility.
         * Set it to {@code false} when unproxyable bean types should always lead to a deployment problem.
         */
        public Builder setStrictCompatibility(boolean strictCompatibility) {
            this.strictCompatibility = strictCompatibility;
            return this;
        }

        /**
         *
         * @param value
         * @return self
         */
        public Builder setOptimizeContexts(boolean value) {
            return setOptimizeContexts(new Predicate<BeanDeployment>() {
                @Override
                public boolean test(BeanDeployment t) {
                    return value;
                }
            });
        }

        /**
         *
         * @param fun
         * @return self
         */
        public Builder setOptimizeContexts(Predicate<BeanDeployment> fun) {
            this.optimizeContexts = fun;
            return this;
        }

        /**
         * Can be used to compute a priority of an alternative bean. A non-null computed value always
         * takes precedence over the priority defined by {@link Priority} or a stereotype.
         *
         * @param priorities
         * @return self
         */
        public Builder setAlternativePriorities(AlternativePriorities priorities) {
            this.alternativePriorities = priorities;
            return this;
        }

        /**
         * Specify the types that should be excluded from discovery.
         *
         * @param predicate
         * @return self
         */
        public Builder addExcludeType(Predicate<ClassInfo> predicate) {
            this.excludeTypes.add(predicate);
            return this;
        }

        /**
         * A generator can contribute to the generated {@link InjectableBean#isSuppressed()} method body.
         *
         * @param generator
         * @return self
         */
        public Builder addSuppressConditionGenerator(Function<BeanInfo, Consumer<BytecodeCreator>> generator) {
            this.suppressConditionGenerators.add(generator);
            return this;
        }

        public Builder setBuildCompatibleExtensions(ExtensionsEntryPoint buildCompatibleExtensions) {
            this.buildCompatibleExtensions = buildCompatibleExtensions;
            return this;
        }

        public BeanProcessor build() {
            return new BeanProcessor(this);
        }

    }

    static <E extends BuildExtension> List<E> initAndSort(List<E> extensions, BuildContext buildContext) {
        for (Iterator<E> iterator = extensions.iterator(); iterator.hasNext();) {
            if (!iterator.next().initialize(buildContext)) {
                iterator.remove();
            }
        }
        extensions.sort(BuildExtension::compare);
        return extensions;
    }

    static class BuildContextImpl implements BuildContext {

        private final Map<String, Object> data = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <V> V get(Key<V> key) {
            return (V) data.get(key.asString());
        }

        @Override
        public <V> V put(Key<V> key, V value) {
            String keyStr = key.asString();
            if (keyStr.startsWith(Key.BUILT_IN_PREFIX)) {
                throw new IllegalArgumentException("Key may not start wit " + Key.BUILT_IN_PREFIX + ": " + keyStr);
            }
            return putInternal(keyStr, value);
        }

        @SuppressWarnings("unchecked")
        <V> V putInternal(Key<V> key, V value) {
            return (V) data.put(key.asString(), value);
        }

        @SuppressWarnings("unchecked")
        <V> V putInternal(String key, V value) {
            return (V) data.put(key, value);
        }

    }

    static class PrivateMembersCollector {

        private final List<String> appDescriptions;
        private final List<String> fwkDescriptions;

        public PrivateMembersCollector() {
            this.appDescriptions = new CopyOnWriteArrayList<>();
            this.fwkDescriptions = LOGGER.isDebugEnabled() ? new CopyOnWriteArrayList<>() : null;
        }

        void add(boolean isApplicationClass, String description) {
            if (isApplicationClass) {
                appDescriptions.add(description);
            } else if (fwkDescriptions != null) {
                fwkDescriptions.add(description);
            }
        }

        private void log() {
            // Log application problems
            if (!appDescriptions.isEmpty()) {
                int limit = LOGGER.isDebugEnabled() ? Integer.MAX_VALUE : 3;
                String info = appDescriptions.stream().limit(limit).map(d -> "\t- " + d).collect(Collectors.joining(",\n"));
                if (appDescriptions.size() > limit) {
                    info += "\n\t- and " + (appDescriptions.size() - limit)
                            + " more - please enable debug logging to see the full list";
                }
                LOGGER.infof(
                        "Found unrecommended usage of private members (use package-private instead) in application beans:%n%s",
                        info);
            }
            // Log fwk problems
            if (fwkDescriptions != null && !fwkDescriptions.isEmpty()) {
                LOGGER.debugf(
                        "Found unrecommended usage of private members (use package-private instead) in framework beans:%n%s",
                        fwkDescriptions.stream().map(d -> "\t- " + d).collect(Collectors.joining(",\n")));
            }
        }

    }

}
