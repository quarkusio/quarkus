package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.AsyncObserverExceptionHandler;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanTypeExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AlternativePriorities;
import io.quarkus.arc.processor.AnnotationLiteralProcessor;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanGenerator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.Beans;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.BytecodeTransformer;
import io.quarkus.arc.processor.ContextConfigurator;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InjectionPointInfo.TypeAndQualifiers;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.processor.ReflectionRegistration;
import io.quarkus.arc.processor.ResourceOutput;
import io.quarkus.arc.processor.StereotypeInfo;
import io.quarkus.arc.processor.StereotypeRegistrar;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.arc.processor.Types;
import io.quarkus.arc.runtime.AdditionalBean;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.LaunchModeProducer;
import io.quarkus.arc.runtime.LoggerProducer;
import io.quarkus.arc.runtime.appcds.AppCDSRecorder;
import io.quarkus.arc.runtime.context.ArcContextProvider;
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
import io.quarkus.deployment.pkg.builditem.AppCDSControlPointBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSRequestedBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.test.TestApplicationClassPredicate;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.smallrye.context.deployment.spi.ThreadContextProviderBuildItem;

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
    static final DotName ASYNC_OBSERVER_EXCEPTION_HANDLER = DotName.createSimple(AsyncObserverExceptionHandler.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CDI);
    }

    @BuildStep
    AdditionalBeanBuildItem quarkusApplication(CombinedIndexBuildItem combinedIndex) {
        List<String> quarkusApplications = new ArrayList<>();
        for (ClassInfo quarkusApplication : combinedIndex.getIndex()
                .getAllKnownImplementors(DotName.createSimple(QuarkusApplication.class.getName()))) {
            if (quarkusApplication.declaredAnnotation(DotNames.DECORATOR) == null) {
                quarkusApplications.add(quarkusApplication.name().toString());
            }
        }

        return AdditionalBeanBuildItem.builder().setUnremovable()
                .setDefaultScope(DotName.createSimple(ApplicationScoped.class.getName()))
                .addBeanClasses(quarkusApplications)
                .build();
    }

    @BuildStep
    StereotypeRegistrarBuildItem convertLegacyAdditionalStereotypes(List<AdditionalStereotypeBuildItem> buildItems) {
        return new StereotypeRegistrarBuildItem(new StereotypeRegistrar() {
            @Override
            public Set<DotName> getAdditionalStereotypes() {
                Set<DotName> result = new HashSet<>();
                for (AdditionalStereotypeBuildItem buildItem : buildItems) {
                    result.addAll(buildItem.getStereotypes()
                            .values()
                            .stream()
                            .flatMap(Collection::stream)
                            .map(AnnotationInstance::name)
                            .collect(Collectors.toSet()));
                }
                return result;
            }
        });
    }

    // PHASE 1 - build BeanProcessor
    @BuildStep
    public ContextRegistrationPhaseBuildItem initialize(
            ArcConfig arcConfig,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            CombinedIndexBuildItem combinedIndex,
            ApplicationIndexBuildItem applicationIndex,
            List<ExcludedTypeBuildItem> excludedTypes,
            List<AnnotationsTransformerBuildItem> annotationTransformers,
            List<InjectionPointTransformerBuildItem> injectionPointTransformers,
            List<ObserverTransformerBuildItem> observerTransformers,
            List<InterceptorBindingRegistrarBuildItem> interceptorBindingRegistrars,
            List<QualifierRegistrarBuildItem> qualifierRegistrars,
            List<StereotypeRegistrarBuildItem> stereotypeRegistrars,
            List<ApplicationClassPredicateBuildItem> applicationClassPredicates,
            List<AdditionalBeanBuildItem> additionalBeans,
            List<ResourceAnnotationBuildItem> resourceAnnotations,
            List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations,
            List<SuppressConditionGeneratorBuildItem> suppressConditionGenerators,
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
                if (customScopes.isScopeIn(transformationContext.getAnnotations())) {
                    // if one of annotations (even if added via transformer) is a scope, no action is needed
                    return;
                }
                DotName defaultScope = additionalBeanTypes.get(beanClassName);
                if (defaultScope != null) {
                    transformationContext.transform().add(defaultScope).done();
                } else {
                    if (!beanClass.annotationsMap().containsKey(ADDITIONAL_BEAN)) {
                        // Add special stereotype is added so that @Dependent is automatically used even if no scope is declared
                        // Otherwise the bean class would be ignored during bean discovery
                        transformationContext.transform().add(ADDITIONAL_BEAN).done();
                    }
                }
            }
        });

        builder.setComputingBeanArchiveIndex(index);
        builder.setImmutableBeanArchiveIndex(beanArchiveIndex.getImmutableIndex());
        builder.setApplicationIndex(combinedIndex.getIndex());
        List<BeanDefiningAnnotation> beanDefiningAnnotations = additionalBeanDefiningAnnotations.stream()
                .map((s) -> new BeanDefiningAnnotation(s.getName(), s.getDefaultScope())).collect(Collectors.toList());
        beanDefiningAnnotations.add(new BeanDefiningAnnotation(ADDITIONAL_BEAN, null));
        builder.setAdditionalBeanDefiningAnnotations(beanDefiningAnnotations);
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
        // register additional stereotypes
        for (StereotypeRegistrarBuildItem registrar : stereotypeRegistrars) {
            builder.addStereotypeRegistrar(registrar.getStereotypeRegistrar());
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
        builder.setFailOnInterceptedPrivateMethod(arcConfig.failOnInterceptedPrivateMethod);
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
        if (!excludedTypes.isEmpty()) {
            for (Predicate<ClassInfo> predicate : initClassPredicates(
                    excludedTypes.stream().map(ExcludedTypeBuildItem::getMatch).collect(Collectors.toList()))) {
                builder.addExcludeType(predicate);
            }
        }

        for (SuppressConditionGeneratorBuildItem generator : suppressConditionGenerators) {
            builder.addSuppressConditionGenerator(generator.getGenerator());
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
            List<BeanConfiguratorBuildItem> beanConfigurators,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {

        for (BeanConfiguratorBuildItem configurator : beanConfigurators) {
            // Just make sure the configurator is processed
            configurator.getValues().forEach(BeanConfigurator::done);
        }

        // Initialize the type -> bean map
        beanRegistrationPhase.getBeanProcessor().getBeanDeployment().initBeanByTypeMap();

        // Register a synthetic bean for each List<?> with qualifier @All
        List<InjectionPointInfo> listAll = beanRegistrationPhase.getInjectionPoints().stream()
                .filter(this::isListAllInjectionPoint).collect(Collectors.toList());
        for (InjectionPointInfo injectionPoint : listAll) {
            // Note that at this point we can be sure that the required type is List<>
            Type typeParam = injectionPoint.getType().asParameterizedType().arguments().get(0);
            if (typeParam.kind() == Type.Kind.WILDCARD_TYPE) {
                ClassInfo declaringClass;
                if (injectionPoint.isField()) {
                    declaringClass = injectionPoint.getTarget().asField().declaringClass();
                } else {
                    declaringClass = injectionPoint.getTarget().asMethod().declaringClass();
                }
                if (declaringClass.declaredAnnotation(DotNames.KOTLIN_METADATA_ANNOTATION) != null) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new DefinitionException(
                                    "kotlin.collections.List cannot be used together with the @All qualifier, please use MutableList or java.util.List instead: "
                                            + injectionPoint.getTargetInfo())));
                } else {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new DefinitionException(
                                    "Wildcard is not a legal type argument for " + injectionPoint.getTargetInfo())));
                }
            } else if (typeParam.kind() == Type.Kind.TYPE_VARIABLE) {
                validationErrors.produce(new ValidationErrorBuildItem(new DefinitionException(
                        "Type variable is not a legal type argument for " + injectionPoint.getTargetInfo())));
            }
        }
        if (!listAll.isEmpty()) {
            registerListInjectionPointsBeans(beanRegistrationPhase, listAll, reflectiveMethods, reflectiveFields,
                    unremovableBeans);
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
    public PreBeanContainerBuildItem generateResources(ArcConfig config, ArcRecorder recorder,
            ShutdownContextBuildItem shutdown,
            ValidationPhaseBuildItem validationPhase,
            List<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LiveReloadBuildItem liveReloadBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer,
            List<ReflectiveBeanClassBuildItem> reflectiveBeanClasses,
            Optional<CurrentContextFactoryBuildItem> currentContextFactory,
            ExecutorService buildExecutor) throws Exception {

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
        Set<DotName> reflectiveBeanClassesNames = reflectiveBeanClasses.stream().map(ReflectiveBeanClassBuildItem::getClassName)
                .collect(Collectors.toSet());

        boolean parallelResourceGeneration = Boolean
                .parseBoolean(System.getProperty("quarkus.arc.parallel-resource-generation", "true"));
        long start = System.nanoTime();
        ExecutorService executor = parallelResourceGeneration ? buildExecutor : null;
        List<ResourceOutput.Resource> resources;
        resources = beanProcessor.generateResources(new ReflectionRegistration() {
            @Override
            public void registerMethod(MethodInfo methodInfo) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(methodInfo));
            }

            @Override
            public void registerField(FieldInfo fieldInfo) {
                reflectiveFields.produce(new ReflectiveFieldBuildItem(fieldInfo));
            }

            @Override
            public void registerClientProxy(DotName beanClassName, String clientProxyName) {
                if (reflectiveBeanClassesNames.contains(beanClassName)) {
                    // Fields should never be registered for client proxies
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, clientProxyName));
                }
            }

            @Override
            public void registerSubclass(DotName beanClassName, String subclassName) {
                if (reflectiveBeanClassesNames.contains(beanClassName)) {
                    // Fields should never be registered for subclasses
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, subclassName));
                }
            }

        }, existingClasses.existingClasses, bytecodeTransformerConsumer,
                config.shouldEnableBeanRemoval() && config.detectUnusedFalsePositives, executor);

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
        LOGGER.debugf("Generated %s resources in %s ms", resources.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

        // Register all qualifiers for reflection to support type-safe resolution at runtime in native image
        for (ClassInfo qualifier : beanProcessor.getBeanDeployment().getQualifiers()) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, qualifier.name().toString()));
        }

        // Register all interceptor bindings for reflection so that AnnotationLiteral.equals() works in a native image
        for (ClassInfo binding : beanProcessor.getBeanDeployment().getInterceptorBindings()) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, binding.name().toString()));
        }

        ArcContainer container = recorder.initContainer(shutdown,
                currentContextFactory.isPresent() ? currentContextFactory.get().getFactory() : null);
        BeanContainer beanContainer = recorder.initBeanContainer(container,
                beanContainerListenerBuildItems.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener)
                        .collect(Collectors.toList()));

        return new PreBeanContainerBuildItem(beanContainer);
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    public void signalBeanContainerReady(AppCDSRecorder recorder, PreBeanContainerBuildItem bi,
            Optional<AppCDSRequestedBuildItem> appCDSRequested,
            BuildProducer<AppCDSControlPointBuildItem> appCDSControlPointProducer,
            BuildProducer<BeanContainerBuildItem> beanContainerProducer) {
        if (appCDSRequested.isPresent()) {
            recorder.controlGenerationAndExit();
            appCDSControlPointProducer.produce(new AppCDSControlPointBuildItem());
        }
        beanContainerProducer.produce(new BeanContainerBuildItem(bi.getValue()));
    }

    @BuildStep(onlyIf = IsTest.class)
    public AdditionalBeanBuildItem testApplicationClassPredicateBean() {
        // We need to register the bean implementation for TestApplicationClassPredicate
        // TestApplicationClassPredicate is used programmatically in the ArC recorder when StartupEvent is fired
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
                new AdditionalApplicationArchiveMarkerBuildItem("META-INF/services/jakarta.enterprise.inject.spi.Extension"));
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
        return new AdditionalBeanBuildItem(LoggerProducer.class);
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

    @BuildStep
    AnnotationsTransformerBuildItem transformListAllInjectionPoints() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE;
            }

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.FIELD || kind == Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (Annotations.contains(ctx.getAnnotations(), DotNames.ALL)) {
                    // For each injection point annotated with @All add a synthetic qualifier
                    AnnotationTarget target = ctx.getTarget();
                    if (target.kind() == Kind.FIELD) {
                        // Identifier is a hash of "type + field annotations"
                        String id = HashUtil
                                .sha1(target.asField().type().toString() + target.asField().annotations().toString());
                        ctx.transform().add(DotNames.IDENTIFIED,
                                AnnotationValue.createStringValue("value", id)).done();
                    } else {
                        MethodInfo method = target.asMethod();
                        Set<AnnotationInstance> alls = Annotations.getAnnotations(Kind.METHOD_PARAMETER, DotNames.ALL,
                                ctx.getAnnotations());
                        Set<AnnotationInstance> paramsAnnotations = Annotations.getAnnotations(Kind.METHOD_PARAMETER,
                                ctx.getAnnotations());
                        List<AnnotationInstance> toAdd = new ArrayList<>();
                        for (AnnotationInstance annotation : alls) {
                            short position = annotation.target().asMethodParameter().position();
                            Set<AnnotationInstance> paramAnnotations = new HashSet<>();
                            for (AnnotationInstance paramAnnotation : paramsAnnotations) {
                                if (paramAnnotation.target().asMethodParameter().position() == position) {
                                    paramAnnotations.add(paramAnnotation);
                                }
                            }
                            // Identifier is a hash of "type + method param annotations"
                            String id = HashUtil.sha1(method.parameterType(position) + paramAnnotations.toString());
                            toAdd.add(
                                    AnnotationInstance.create(DotNames.IDENTIFIED,
                                            MethodParameterInfo.create(method,
                                                    annotation.target().asMethodParameter().position()),
                                            new AnnotationValue[] { AnnotationValue.createStringValue("value", id) }));
                        }
                        Transformation transform = ctx.transform();
                        toAdd.forEach(transform::add);
                        transform.done();
                    }
                }
            }

        });
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableAsyncObserverExceptionHandlers() {
        // Make all classes implementing AsyncObserverExceptionHandler unremovable
        return UnremovableBeanBuildItem.beanTypes(Set.of(ASYNC_OBSERVER_EXCEPTION_HANDLER));
    }

    @BuildStep
    void validateAsyncObserverExceptionHandlers(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> errors) {
        BeanResolver resolver = validationPhase.getBeanProcessor().getBeanDeployment().getBeanResolver();
        try {
            BeanInfo bean = resolver.resolveAmbiguity(
                    resolver.resolveBeans(Type.create(ASYNC_OBSERVER_EXCEPTION_HANDLER, org.jboss.jandex.Type.Kind.CLASS)));
            if (bean == null) {
                // This should never happen because of the default impl
                errors.produce(new ValidationErrorBuildItem(
                        new UnsatisfiedResolutionException("AsyncObserverExceptionHandler bean not found")));
            }
        } catch (AmbiguousResolutionException e) {
            errors.produce(new ValidationErrorBuildItem(e));
        }
    }

    @BuildStep
    void registerContextPropagation(ArcConfig config, BuildProducer<ThreadContextProviderBuildItem> threadContextProvider) {
        if (config.contextPropagation.enabled) {
            threadContextProvider.produce(new ThreadContextProviderBuildItem(ArcContextProvider.class));
        }
    }

    private void registerListInjectionPointsBeans(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            List<InjectionPointInfo> injectionPoints, BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        BeanDeployment beanDeployment = beanRegistrationPhase.getBeanProcessor().getBeanDeployment();
        AnnotationLiteralProcessor annotationLiterals = beanRegistrationPhase.getBeanProcessor()
                .getAnnotationLiteralProcessor();
        ReflectionRegistration reflectionRegistration = new ReflectionRegistration() {
            @Override
            public void registerMethod(MethodInfo methodInfo) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(methodInfo));
            }

            @Override
            public void registerField(FieldInfo fieldInfo) {
                reflectiveFields.produce(new ReflectiveFieldBuildItem(fieldInfo));
            }

        };

        List<TypeAndQualifiers> unremovables = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (InjectionPointInfo injectionPoint : injectionPoints) {

            // The injection point must be registered immediately and NOT inside the creator callback
            if (injectionPoint.isField()) {
                reflectionRegistration.registerField(injectionPoint.getTarget().asField());
            } else {
                reflectionRegistration.registerMethod(injectionPoint.getTarget().asMethod());
            }

            AnnotationInstance identifiedAnnotation = injectionPoint.getRequiredQualifier(DotNames.IDENTIFIED);
            if (identifiedAnnotation == null
                    // The id is a hash of "type + all annotations" - if there's an exact match then we don't need to add another bean
                    || !ids.add(identifiedAnnotation.value().asString())) {
                continue;
            }

            // All qualifiers but @All and @Identified
            Set<AnnotationInstance> qualifiers = new HashSet<>(injectionPoint.getRequiredQualifiers());
            for (Iterator<AnnotationInstance> it = qualifiers.iterator(); it.hasNext();) {
                AnnotationInstance qualifier = it.next();
                if (DotNames.ALL.equals(qualifier.name()) || DotNames.IDENTIFIED.equals(qualifier.name())) {
                    it.remove();
                }
            }
            if (qualifiers.isEmpty()) {
                // If no other qualifier is used then add @Any
                qualifiers.add(AnnotationInstance.create(DotNames.ANY, null, new AnnotationValue[] {}));
            }

            Type elementType = injectionPoint.getType().asParameterizedType().arguments().get(0);

            unremovables.add(new TypeAndQualifiers(
                    elementType.name().equals(DotNames.INSTANCE_HANDLE)
                            ? elementType.asParameterizedType().arguments().get(0)
                            : elementType,
                    qualifiers));

            BeanConfigurator<?> configurator = beanRegistrationPhase.getContext()
                    .configure(List.class)
                    .forceApplicationClass()
                    .scope(BuiltinScope.DEPENDENT.getInfo())
                    .addType(injectionPoint.getRequiredType());

            injectionPoint.getRequiredQualifiers().forEach(configurator::addQualifier);

            if (injectionPoint.getTargetBean().isPresent()) {
                // Generate the bean class in the same package as the target bean
                configurator.targetPackageName(injectionPoint.getTargetBean().get().getTargetPackageName());
            }

            configurator.creator(mc -> {
                ResultHandle injectionPointType = Types.getTypeHandle(mc, injectionPoint.getType());

                // List<T> or List<InstanceHandle<T>
                ResultHandle requiredType;
                MethodDescriptor instancesMethod;
                if (elementType.name().equals(DotNames.INSTANCE_HANDLE)) {
                    requiredType = Types.getTypeHandle(mc, elementType.asParameterizedType().arguments().get(0));
                    instancesMethod = MethodDescriptors.INSTANCES_LIST_OF_HANDLES;
                } else {
                    requiredType = Types.getTypeHandle(mc, elementType);
                    instancesMethod = MethodDescriptors.INSTANCES_LIST_OF;
                }

                ResultHandle requiredQualifiers = BeanGenerator.collectQualifiers(null, null,
                        beanDeployment, mc, annotationLiterals, qualifiers);
                ResultHandle injectionPointAnnotations = BeanGenerator.collectInjectionPointAnnotations(null, null,
                        beanDeployment,
                        mc, injectionPoint, annotationLiterals,
                        beanRegistrationPhase.getBeanProcessor().getInjectionPointAnnotationsPredicate());
                ResultHandle javaMember = BeanGenerator.getJavaMemberHandle(mc, injectionPoint,
                        ReflectionRegistration.NOOP);
                ResultHandle container = mc
                        .invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
                ResultHandle targetBean = mc.invokeInterfaceMethod(
                        MethodDescriptors.ARC_CONTAINER_BEAN,
                        container,
                        injectionPoint.getTargetBean().isPresent()
                                ? mc.load(injectionPoint.getTargetBean().get().getIdentifier())
                                : mc.loadNull());

                ResultHandle ret = mc.invokeStaticMethod(instancesMethod, targetBean,
                        injectionPointType, requiredType, requiredQualifiers, mc.getMethodParam(0),
                        injectionPointAnnotations,
                        javaMember, mc.load(injectionPoint.getPosition()));
                mc.returnValue(ret);
            });
            configurator.done();
        }
        if (!unremovables.isEmpty()) {
            // New beans were registered - we need to re-init the type -> bean map
            // Also make all beans that match the List<> injection points unremovable
            beanDeployment.initBeanByTypeMap();
            // And make all the matching beans unremovable
            unremovableBeans.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
                @Override
                public boolean test(BeanInfo bean) {
                    for (TypeAndQualifiers tq : unremovables) {
                        if (Beans.matches(bean, tq)) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }
    }

    private boolean isListAllInjectionPoint(InjectionPointInfo injectionPoint) {
        return DotNames.LIST.equals(injectionPoint.getRequiredType().name())
                && Annotations.contains(injectionPoint.getRequiredQualifiers(), DotNames.ALL);
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
