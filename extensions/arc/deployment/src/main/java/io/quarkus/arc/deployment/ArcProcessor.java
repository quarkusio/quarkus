package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanTypeExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AlternativePriorities;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BytecodeTransformer;
import io.quarkus.arc.processor.ContextConfigurator;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.processor.ReflectionRegistration;
import io.quarkus.arc.processor.ResourceOutput;
import io.quarkus.arc.processor.StereotypeInfo;
import io.quarkus.arc.runtime.AdditionalBean;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.JBossLoggerProducer;
import io.quarkus.arc.runtime.JavaUtilLoggerProducer;
import io.quarkus.arc.runtime.LaunchModeProducer;
import io.quarkus.arc.runtime.test.PreloadedTestApplicationClassPredicate;
import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.test.TestApplicationClassPredicate;

/**
 * This class contains build steps that trigger various phases of the bean processing.
 * <p>
 * Other build steps can either register "configuring" build items, such as {@link AdditionalBeanBuildItem} or inject build
 * items representing particular phases:
 * <ol>
 * <li>{@link ContextRegistrationPhaseBuildItem}</li>
 * <li>{@link BeanRegistrationPhaseBuildItem}</li>
 * <li>{@link ObserverRegistrationPhaseBuildItem}</li>
 * <li>{@link ValidationPhaseBuildItem}</li>
 * </ol>
 * These build items are especially useful if an extension needs to produce other build items within the given phase.
 *
 * @see BeanProcessor
 */
public class ArcProcessor {

    private static final Logger LOGGER = Logger.getLogger(ArcProcessor.class);

    static final DotName ADDITIONAL_BEAN = DotName.createSimple(AdditionalBean.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CDI);
    }

    @BuildStep
    AdditionalBeanBuildItem quarkusApplication(CombinedIndexBuildItem combinedIndex) {
        List<String> quarkusApplications = new ArrayList<>();
        for (ClassInfo quarkusApplication : combinedIndex.getIndex()
                .getAllKnownImplementors(DotName.createSimple(QuarkusApplication.class.getName()))) {
            if (quarkusApplication.classAnnotation(DotNames.DECORATOR) == null) {
                quarkusApplications.add(quarkusApplication.name().toString());
            }
        }
        return AdditionalBeanBuildItem.builder().setUnremovable()
                .setDefaultScope(DotName.createSimple(ApplicationScoped.class.getName()))
                .addBeanClasses(quarkusApplications)
                .build();
    }

    // PHASE 1 - build BeanProcessor
    @BuildStep
    public ContextRegistrationPhaseBuildItem initialize(
            ArcConfig arcConfig,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            CombinedIndexBuildItem combinedIndex,
            ApplicationIndexBuildItem applicationIndex,
            List<AnnotationsTransformerBuildItem> annotationTransformers,
            List<InjectionPointTransformerBuildItem> injectionPointTransformers,
            List<ObserverTransformerBuildItem> observerTransformers,
            List<InterceptorBindingRegistrarBuildItem> interceptorBindingRegistrars,
            List<QualifierRegistrarBuildItem> qualifierRegistrars,
            List<AdditionalStereotypeBuildItem> additionalStereotypeBuildItems,
            List<ApplicationClassPredicateBuildItem> applicationClassPredicates,
            List<AdditionalBeanBuildItem> additionalBeans,
            List<ResourceAnnotationBuildItem> resourceAnnotations,
            List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations,
            Optional<TestClassPredicateBuildItem> testClassPredicate,
            Capabilities capabilities,
            CustomScopeAnnotationsBuildItem customScopes,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<CompletedApplicationClassPredicateBuildItem> applicationClassPredicateProducer) {

        if (!arcConfig.isRemoveUnusedBeansFieldValid()) {
            throw new IllegalArgumentException("Invalid configuration value set for 'quarkus.arc.remove-unused-beans'." +
                    " Please use one of " + ArcConfig.ALLOWED_REMOVE_UNUSED_BEANS_VALUES);
        }

        // bean type -> default scope (may be null)
        Map<String, DotName> additionalBeanTypes = new HashMap<>();
        for (AdditionalBeanBuildItem additionalBean : additionalBeans) {
            DotName defaultScope = additionalBean.getDefaultScope();
            for (String beanClass : additionalBean.getBeanClasses()) {
                DotName existingDefaultScope = additionalBeanTypes.get(beanClass);
                if (existingDefaultScope != null && defaultScope != null && !existingDefaultScope.equals(defaultScope)) {
                    throw new IllegalStateException("Different default scopes defined for additional bean class: " + beanClass
                            + "\n\t - scopes: " + defaultScope + " and "
                            + existingDefaultScope);
                }
                additionalBeanTypes.put(beanClass, defaultScope);
            }
        }

        Set<DotName> generatedClassNames = beanArchiveIndex.getGeneratedClassNames();
        IndexView index = beanArchiveIndex.getIndex();
        BeanProcessor.Builder builder = BeanProcessor.builder();
        IndexView applicationClassesIndex = applicationIndex.getIndex();
        Predicate<DotName> applicationClassPredicate = new AbstractCompositeApplicationClassesPredicate<DotName>(
                applicationClassesIndex, generatedClassNames, applicationClassPredicates, testClassPredicate) {
            @Override
            protected DotName getDotName(DotName dotName) {
                return dotName;
            }
        };
        applicationClassPredicateProducer.produce(new CompletedApplicationClassPredicateBuildItem(applicationClassPredicate));
        builder.setApplicationClassPredicate(applicationClassPredicate);

        builder.addAnnotationTransformer(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return AnnotationTarget.Kind.CLASS == kind;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                ClassInfo beanClass = transformationContext.getTarget().asClass();
                String beanClassName = beanClass.name().toString();
                if (!additionalBeanTypes.containsKey(beanClassName)) {
                    // Not an additional bean type
                    return;
                }
                if (customScopes.isScopeDeclaredOn(beanClass)) {
                    // If it declares a scope no action is needed
                    return;
                }
                DotName defaultScope = additionalBeanTypes.get(beanClassName);
                if (defaultScope != null) {
                    transformationContext.transform().add(defaultScope).done();
                } else {
                    if (!beanClass.annotations().containsKey(ADDITIONAL_BEAN)) {
                        // Add special stereotype is added so that @Dependent is automatically used even if no scope is declared
                        // Otherwise the bean class would be ignored during bean discovery
                        transformationContext.transform().add(ADDITIONAL_BEAN).done();
                    }
                }
            }
        });

        builder.setBeanArchiveIndex(index);
        builder.setApplicationIndex(combinedIndex.getIndex());
        List<BeanDefiningAnnotation> beanDefiningAnnotations = additionalBeanDefiningAnnotations.stream()
                .map((s) -> new BeanDefiningAnnotation(s.getName(), s.getDefaultScope())).collect(Collectors.toList());
        beanDefiningAnnotations.add(new BeanDefiningAnnotation(ADDITIONAL_BEAN, null));
        builder.setAdditionalBeanDefiningAnnotations(beanDefiningAnnotations);
        final Map<DotName, Collection<AnnotationInstance>> additionalStereotypes = new HashMap<>();
        for (final AdditionalStereotypeBuildItem item : additionalStereotypeBuildItems) {
            additionalStereotypes.putAll(item.getStereotypes());
        }
        builder.setAdditionalStereotypes(additionalStereotypes);
        builder.setSharedAnnotationLiterals(true);
        builder.addResourceAnnotations(
                resourceAnnotations.stream().map(ResourceAnnotationBuildItem::getName).collect(Collectors.toList()));
        // register all annotation transformers
        for (AnnotationsTransformerBuildItem transformer : annotationTransformers) {
            builder.addAnnotationTransformer(transformer.getAnnotationsTransformer());
        }
        // register all injection point transformers
        for (InjectionPointTransformerBuildItem transformer : injectionPointTransformers) {
            builder.addInjectionPointTransformer(transformer.getInjectionPointsTransformer());
        }
        // register all observer transformers
        for (ObserverTransformerBuildItem transformer : observerTransformers) {
            builder.addObserverTransformer(transformer.getInstance());
        }
        // register additional interceptor bindings
        for (InterceptorBindingRegistrarBuildItem registrar : interceptorBindingRegistrars) {
            builder.addInterceptorBindingRegistrar(registrar.getInterceptorBindingRegistrar());
        }
        // register additional qualifiers
        for (QualifierRegistrarBuildItem registrar : qualifierRegistrars) {
            builder.addQualifierRegistrar(registrar.getQualifierRegistrar());
        }
        builder.setRemoveUnusedBeans(arcConfig.shouldEnableBeanRemoval());
        if (arcConfig.shouldOnlyKeepAppBeans()) {
            builder.addRemovalExclusion(new AbstractCompositeApplicationClassesPredicate<BeanInfo>(
                    applicationClassesIndex, generatedClassNames, applicationClassPredicates, testClassPredicate) {
                @Override
                protected DotName getDotName(BeanInfo bean) {
                    return bean.getBeanClass();
                }
            });
        }
        builder.addRemovalExclusion(new BeanTypeExclusion(DotName.createSimple(TestApplicationClassPredicate.class.getName())));
        for (AdditionalBeanBuildItem additionalBean : additionalBeans) {
            if (!additionalBean.isRemovable()) {
                for (String beanClass : additionalBean.getBeanClasses()) {
                    builder.addRemovalExclusion(new BeanClassNameExclusion(beanClass));
                }
            }
        }
        for (BeanDefiningAnnotationBuildItem annotation : additionalBeanDefiningAnnotations) {
            if (!annotation.isRemovable()) {
                builder.addRemovalExclusion(new BeanClassAnnotationExclusion(annotation.getName()));
            }
        }
        // unremovable beans specified in application.properties
        if (arcConfig.unremovableTypes.isPresent()) {
            List<Predicate<ClassInfo>> classPredicates = initClassPredicates(arcConfig.unremovableTypes.get());
            builder.addRemovalExclusion(new Predicate<BeanInfo>() {
                @Override
                public boolean test(BeanInfo beanInfo) {
                    ClassInfo beanClass = beanInfo.getImplClazz();
                    if (beanClass != null) {
                        // if any of the predicates match, we make the given bean unremovable
                        for (Predicate<ClassInfo> predicate : classPredicates) {
                            if (predicate.test(beanClass)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }
        if (testClassPredicate.isPresent()) {
            builder.addRemovalExclusion(new Predicate<BeanInfo>() {
                @Override
                public boolean test(BeanInfo bean) {
                    return testClassPredicate.get().getPredicate().test(bean.getBeanClass().toString());
                }
            });
        }
        builder.setTransformUnproxyableClasses(arcConfig.transformUnproxyableClasses);
        builder.setJtaCapabilities(capabilities.isPresent(Capability.TRANSACTIONS));
        builder.setGenerateSources(BootstrapDebug.DEBUG_SOURCES_DIR != null);
        builder.setAllowMocking(launchModeBuildItem.getLaunchMode() == LaunchMode.TEST);

        if (arcConfig.selectedAlternatives.isPresent()) {
            final List<Predicate<ClassInfo>> selectedAlternatives = initClassPredicates(
                    arcConfig.selectedAlternatives.get());
            builder.setAlternativePriorities(new AlternativePriorities() {

                @Override
                public Integer compute(AnnotationTarget target, Collection<StereotypeInfo> stereotypes) {
                    ClassInfo clazz;
                    switch (target.kind()) {
                        case CLASS:
                            clazz = target.asClass();
                            break;
                        case FIELD:
                            clazz = target.asField().declaringClass();
                            break;
                        case METHOD:
                            clazz = target.asMethod().declaringClass();
                            break;
                        default:
                            return null;
                    }
                    if (selectedAlternatives.stream().anyMatch(p -> p.test(clazz))) {
                        return Integer.MAX_VALUE;
                    }
                    if (!stereotypes.isEmpty()) {
                        for (StereotypeInfo stereotype : stereotypes) {
                            if (selectedAlternatives.stream().anyMatch(p -> p.test(stereotype.getTarget()))) {
                                return Integer.MAX_VALUE;
                            }
                        }
                    }
                    return null;
                }
            });
        }

        if (arcConfig.excludeTypes.isPresent()) {
            for (Predicate<ClassInfo> predicate : initClassPredicates(
                    arcConfig.excludeTypes.get())) {
                builder.addExcludeType(predicate);
            }
        }

        BeanProcessor beanProcessor = builder.build();
        ContextRegistrar.RegistrationContext context = beanProcessor.registerCustomContexts();
        return new ContextRegistrationPhaseBuildItem(context, beanProcessor);
    }

    // PHASE 2 - register all beans
    @BuildStep
    public BeanRegistrationPhaseBuildItem registerBeans(ContextRegistrationPhaseBuildItem contextRegistrationPhase,
            List<ContextConfiguratorBuildItem> contextConfigurationRegistry,
            BuildProducer<InterceptorResolverBuildItem> interceptorResolver,
            BuildProducer<BeanDiscoveryFinishedBuildItem> beanDiscoveryFinished,
            BuildProducer<TransformedAnnotationsBuildItem> transformedAnnotations) {

        for (ContextConfiguratorBuildItem contextConfigurator : contextConfigurationRegistry) {
            for (ContextConfigurator value : contextConfigurator.getValues()) {
                // Just make sure the configurator is processed
                value.done();
            }
        }
        BeanProcessor beanProcessor = contextRegistrationPhase.getBeanProcessor();
        beanProcessor.registerScopes();
        BeanRegistrar.RegistrationContext registrationContext = beanProcessor.registerBeans();
        BeanDeployment beanDeployment = beanProcessor.getBeanDeployment();
        interceptorResolver.produce(new InterceptorResolverBuildItem(beanDeployment));
        beanDiscoveryFinished.produce(new BeanDiscoveryFinishedBuildItem(beanDeployment));
        transformedAnnotations.produce(new TransformedAnnotationsBuildItem(beanDeployment));

        return new BeanRegistrationPhaseBuildItem(registrationContext, beanProcessor);
    }

    // PHASE 3 - register synthetic observers
    @BuildStep
    public ObserverRegistrationPhaseBuildItem registerSyntheticObservers(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            List<BeanConfiguratorBuildItem> beanConfigurationRegistry) {

        for (BeanConfiguratorBuildItem configurator : beanConfigurationRegistry) {
            // Just make sure the configurator is processed
            configurator.getValues().forEach(BeanConfigurator::done);
        }

        BeanProcessor beanProcessor = beanRegistrationPhase.getBeanProcessor();
        ObserverRegistrar.RegistrationContext registrationContext = beanProcessor.registerSyntheticObservers();

        return new ObserverRegistrationPhaseBuildItem(registrationContext, beanProcessor);
    }

    // PHASE 4 - initialize and validate the bean deployment
    @BuildStep
    public ValidationPhaseBuildItem validate(ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            List<ObserverConfiguratorBuildItem> observerConfigurationRegistry,
            List<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer,
            BuildProducer<SynthesisFinishedBuildItem> synthesisFinished) {

        for (ObserverConfiguratorBuildItem configurator : observerConfigurationRegistry) {
            // Just make sure the configurator is processed
            configurator.getValues().forEach(ObserverConfigurator::done);
        }

        BeanProcessor beanProcessor = observerRegistrationPhase.getBeanProcessor();
        synthesisFinished.produce(new SynthesisFinishedBuildItem(beanProcessor.getBeanDeployment()));

        Consumer<BytecodeTransformer> bytecodeTransformerConsumer = new BytecodeTransformerConsumer(bytecodeTransformer);

        beanProcessor.initialize(bytecodeTransformerConsumer,
                unremovableBeans.stream().map(UnremovableBeanBuildItem::getPredicate).collect(Collectors.toList()));
        BeanDeploymentValidator.ValidationContext validationContext = beanProcessor.validate(bytecodeTransformerConsumer);

        return new ValidationPhaseBuildItem(validationContext, beanProcessor);
    }

    // PHASE 5 - generate resources and initialize the container
    @BuildStep
    @Record(STATIC_INIT)
    public BeanContainerBuildItem generateResources(ArcConfig config, ArcRecorder recorder, ShutdownContextBuildItem shutdown,
            ValidationPhaseBuildItem validationPhase,
            List<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LiveReloadBuildItem liveReloadBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer) throws Exception {

        for (ValidationErrorBuildItem validationError : validationErrors) {
            for (Throwable error : validationError.getValues()) {
                validationPhase.getContext().addDeploymentProblem(error);
            }
        }

        BeanProcessor beanProcessor = validationPhase.getBeanProcessor();
        beanProcessor.processValidationErrors(validationPhase.getContext());
        ExistingClasses existingClasses = liveReloadBuildItem.getContextObject(ExistingClasses.class);
        if (existingClasses == null) {
            existingClasses = new ExistingClasses();
            liveReloadBuildItem.setContextObject(ExistingClasses.class, existingClasses);
        }

        Consumer<BytecodeTransformer> bytecodeTransformerConsumer = new BytecodeTransformerConsumer(bytecodeTransformer);

        long start = System.currentTimeMillis();
        List<ResourceOutput.Resource> resources = beanProcessor.generateResources(new ReflectionRegistration() {
            @Override
            public void registerMethod(MethodInfo methodInfo) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(methodInfo));
            }

            @Override
            public void registerField(FieldInfo fieldInfo) {
                reflectiveFields.produce(new ReflectiveFieldBuildItem(fieldInfo));
            }
        }, existingClasses.existingClasses, bytecodeTransformerConsumer,
                config.shouldEnableBeanRemoval() && config.detectUnusedFalsePositives);
        for (ResourceOutput.Resource resource : resources) {
            switch (resource.getType()) {
                case JAVA_CLASS:
                    LOGGER.debugf("Add %s class: %s", (resource.isApplicationClass() ? "APP" : "FWK"),
                            resource.getFullyQualifiedName());
                    generatedClass.produce(new GeneratedClassBuildItem(resource.isApplicationClass(), resource.getName(),
                            resource.getData(), resource.getSource()));
                    if (!resource.isApplicationClass()) {
                        existingClasses.existingClasses.add(resource.getName());
                    }
                    break;
                case SERVICE_PROVIDER:
                    generatedResource.produce(
                            new GeneratedResourceBuildItem("META-INF/services/" + resource.getName(), resource.getData()));
                    break;
                default:
                    break;
            }
        }
        LOGGER.debugf("Generated %s resources in %s ms", resources.size(), System.currentTimeMillis() - start);

        // Register all qualifiers for reflection to support type-safe resolution at runtime in native image
        for (ClassInfo qualifier : beanProcessor.getBeanDeployment().getQualifiers()) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, qualifier.name().toString()));
        }

        ArcContainer container = recorder.getContainer(shutdown);
        BeanContainer beanContainer = recorder.initBeanContainer(container,
                beanContainerListenerBuildItems.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener)
                        .collect(Collectors.toList()));

        return new BeanContainerBuildItem(beanContainer);
    }

    @BuildStep(onlyIf = IsTest.class)
    public AdditionalBeanBuildItem testApplicationClassPredicateBean() {
        // We need to register the bean implementation for TestApplicationClassPredicate
        // TestApplicationClassPredicate is used programatically in the ArC recorder when StartupEvent is fired
        return AdditionalBeanBuildItem.unremovableOf(PreloadedTestApplicationClassPredicate.class);
    }

    @BuildStep(onlyIf = IsTest.class)
    @Record(ExecutionTime.STATIC_INIT)
    void initTestApplicationClassPredicateBean(ArcRecorder recorder, BeanContainerBuildItem beanContainer,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            CompletedApplicationClassPredicateBuildItem predicate) {
        Set<String> applicationBeanClasses = new HashSet<>();
        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            if (predicate.test(bean.getBeanClass())) {
                applicationBeanClasses.add(bean.getBeanClass().toString());
            }
        }
        recorder.initTestApplicationClassPredicate(applicationBeanClasses);
    }

    @BuildStep
    List<AdditionalApplicationArchiveMarkerBuildItem> marker() {
        return Arrays.asList(new AdditionalApplicationArchiveMarkerBuildItem("META-INF/beans.xml"),
                new AdditionalApplicationArchiveMarkerBuildItem("META-INF/services/javax.enterprise.inject.spi.Extension"));
    }

    @BuildStep
    @Record(value = RUNTIME_INIT)
    void setupExecutor(ExecutorBuildItem executor, ArcRecorder recorder) {
        recorder.initExecutor(executor.getExecutorProxy());
    }

    @BuildStep
    AdditionalBeanBuildItem launchMode() {
        return new AdditionalBeanBuildItem(LaunchModeProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem loggerProducer() {
        return new AdditionalBeanBuildItem(JBossLoggerProducer.class, JavaUtilLoggerProducer.class);
    }

    @BuildStep
    CustomScopeAnnotationsBuildItem exposeCustomScopeNames(List<CustomScopeBuildItem> customScopes) {
        Set<DotName> names = new HashSet<>();
        for (CustomScopeBuildItem customScope : customScopes) {
            names.add(customScope.getAnnotationName());
        }
        return new CustomScopeAnnotationsBuildItem(names);
    }

    private List<Predicate<ClassInfo>> initClassPredicates(List<String> types) {
        final String packMatch = ".*";
        final String packStarts = ".**";
        List<Predicate<ClassInfo>> predicates = new ArrayList<>();
        for (String val : types) {
            if (val.endsWith(packMatch)) {
                // Package matches
                final String pack = val.substring(0, val.length() - packMatch.length());
                predicates.add(new Predicate<ClassInfo>() {
                    @Override
                    public boolean test(ClassInfo c) {
                        return DotNames.packageName(c.name()).equals(pack);
                    }
                });
            } else if (val.endsWith(packStarts)) {
                // Package starts with
                final String prefix = val.substring(0, val.length() - packStarts.length());
                predicates.add(new Predicate<ClassInfo>() {
                    @Override
                    public boolean test(ClassInfo c) {
                        return DotNames.packageName(c.name()).startsWith(prefix);
                    }
                });
            } else if (val.contains(".")) {
                // Fully qualified name matches
                predicates.add(new Predicate<ClassInfo>() {
                    @Override
                    public boolean test(ClassInfo c) {
                        return c.name().toString().equals(val);
                    }
                });
            } else {
                // Simple name matches
                predicates.add(new Predicate<ClassInfo>() {
                    @Override
                    public boolean test(ClassInfo c) {
                        return DotNames.simpleName(c).equals(val);
                    }
                });
            }
        }
        return predicates;
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem quarkusMain() {
        return new BeanDefiningAnnotationBuildItem(DotName.createSimple(QuarkusMain.class.getName()), DotNames.SINGLETON);
    }

    private abstract static class AbstractCompositeApplicationClassesPredicate<T> implements Predicate<T> {

        private final IndexView applicationClassesIndex;
        private final Set<DotName> generatedClassNames;
        private final List<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems;
        private final Optional<TestClassPredicateBuildItem> testClassPredicate;

        protected abstract DotName getDotName(T t);

        private AbstractCompositeApplicationClassesPredicate(IndexView applicationClassesIndex,
                Set<DotName> generatedClassNames,
                List<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems,
                Optional<TestClassPredicateBuildItem> testClassPredicate) {
            this.applicationClassesIndex = applicationClassesIndex;
            this.generatedClassNames = generatedClassNames;
            this.applicationClassPredicateBuildItems = applicationClassPredicateBuildItems;
            this.testClassPredicate = testClassPredicate;
        }

        @Override
        public boolean test(T t) {
            final DotName dotName = getDotName(t);
            if (applicationClassesIndex.getClassByName(dotName) != null) {
                return true;
            }
            if (generatedClassNames.contains(dotName)) {
                return true;
            }
            String className = dotName.toString();
            if (!applicationClassPredicateBuildItems.isEmpty()) {
                for (ApplicationClassPredicateBuildItem predicate : applicationClassPredicateBuildItems) {
                    if (predicate.test(className)) {
                        return true;
                    }
                }
            }
            if (testClassPredicate.isPresent()) {
                if (testClassPredicate.get().getPredicate().test(className)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This tracks beans etc from the platform that have already been generated. There is no need to spend time
     * generating them again on a hot reload
     */
    static class ExistingClasses {
        Set<String> existingClasses = new HashSet<>();
    }

    private static class BytecodeTransformerConsumer implements Consumer<BytecodeTransformer> {

        private final BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer;

        public BytecodeTransformerConsumer(BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer) {
            this.bytecodeTransformer = bytecodeTransformer;
        }

        @Override
        public void accept(BytecodeTransformer t) {
            bytecodeTransformer.produce(new BytecodeTransformerBuildItem(t.getClassToTransform(), t.getVisitorFunction()));
        }
    }
}
