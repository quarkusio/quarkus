package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ContextConfigurator;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.arc.processor.ReflectionRegistration;
import io.quarkus.arc.processor.ResourceOutput;
import io.quarkus.arc.runtime.AdditionalBean;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.LifecycleEventRunner;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveMethodBuildItem;

/**
 * This class contains build steps that trigger various phases of the bean processing.
 * <p>
 * Other build steps can either register "configuring" build items, such as {@link AdditionalBeanBuildItem} or inject build
 * items representing particular phases:
 * <ol>
 * <li>{@link ContextRegistrationPhaseBuildItem}</li>
 * <li>{@link BeanRegistrationPhaseBuildItem}</li>
 * <li>{@link ValidationPhaseBuildItem}</li>
 * </ol>
 * These build items are especially useful if an extension needs to produce other build items within the given phase.
 * 
 * @see BeanProcessor
 */
public class ArcProcessor {

    private static final Logger LOGGER = Logger.getLogger(ArcProcessor.class);

    static final DotName ADDITIONAL_BEAN = DotName.createSimple(AdditionalBean.class.getName());

    // PHASE 1 - build BeanProcessor, register custom contexts 
    @BuildStep
    public ContextRegistrationPhaseBuildItem initialize(
            ArcConfig arcConfig,
            BeanArchiveIndexBuildItem beanArchiveIndex,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<AnnotationsTransformerBuildItem> annotationTransformers,
            List<InjectionPointTransformerBuildItem> injectionPointTransformers,
            List<AdditionalStereotypeBuildItem> additionalStereotypeBuildItems,
            List<ApplicationClassPredicateBuildItem> applicationClassPredicates,
            List<AdditionalBeanBuildItem> additionalBeans,
            List<BeanRegistrarBuildItem> beanRegistrars,
            List<ContextRegistrarBuildItem> contextRegistrars,
            List<BeanDeploymentValidatorBuildItem> beanDeploymentValidators,
            List<ResourceAnnotationBuildItem> resourceAnnotations,
            List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations,
            List<UnremovableBeanBuildItem> removalExclusions,
            Optional<TestClassPredicateBuildItem> testClassPredicate,
            BuildProducer<FeatureBuildItem> feature) {

        if (!arcConfig.isRemoveUnusedBeansFieldValid()) {
            throw new IllegalArgumentException("Invalid configuration value set for 'quarkus.arc.remove-unused-beans'." +
                    " Please use one of " + ArcConfig.ALLOWED_REMOVE_UNUSED_BEANS_VALUES);
        }

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CDI));

        List<String> additionalBeansTypes = beanArchiveIndex.getAdditionalBeans();
        Set<DotName> generatedClassNames = beanArchiveIndex.getGeneratedClassNames();
        IndexView index = beanArchiveIndex.getIndex();
        BeanProcessor.Builder builder = BeanProcessor.builder();
        IndexView applicationClassesIndex = applicationArchivesBuildItem.getRootArchive().getIndex();
        builder.setApplicationClassPredicate(new AbstractCompositeApplicationClassesPredicate<DotName>(
                applicationClassesIndex, generatedClassNames, applicationClassPredicates) {
            @Override
            protected DotName getDotName(DotName dotName) {
                return dotName;
            }
        });
        builder.addAnnotationTransformer(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return AnnotationTarget.Kind.CLASS == kind;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                ClassInfo beanClass = transformationContext.getTarget().asClass();
                String beanClassName = beanClass.name().toString();
                if (additionalBeansTypes.contains(beanClassName)) {
                    if (BuiltinScope.isDeclaredOn(beanClass)) {
                        // If it declares a built-in scope no action is needed
                        return;
                    }
                    // Try to determine the default scope
                    DotName defaultScope = additionalBeans.stream()
                            .filter(ab -> ab.contains(beanClassName)).findFirst().map(AdditionalBeanBuildItem::getDefaultScope)
                            .orElse(null);
                    if (defaultScope == null && !beanClass.annotations().containsKey(ADDITIONAL_BEAN)) {
                        // Add special stereotype so that @Dependent is automatically used even if no scope is declared
                        transformationContext.transform().add(ADDITIONAL_BEAN).done();
                    } else {
                        transformationContext.transform().add(defaultScope).done();
                    }
                }
            }
        });
        builder.setIndex(index);
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
        for (AnnotationsTransformerBuildItem transformerItem : annotationTransformers) {
            builder.addAnnotationTransformer(transformerItem.getAnnotationsTransformer());
        }
        // register all injection point transformers
        for (InjectionPointTransformerBuildItem transformerItem : injectionPointTransformers) {
            builder.addInjectionPointTransformer(transformerItem.getInjectionPointsTransformer());
        }
        for (BeanRegistrarBuildItem item : beanRegistrars) {
            builder.addBeanRegistrar(item.getBeanRegistrar());
        }
        for (ContextRegistrarBuildItem item : contextRegistrars) {
            builder.addContextRegistrar(item.getContextRegistrar());
        }
        for (BeanDeploymentValidatorBuildItem item : beanDeploymentValidators) {
            builder.addBeanDeploymentValidator(item.getBeanDeploymentValidator());
        }
        builder.setRemoveUnusedBeans(arcConfig.shouldEnableBeanRemoval());
        if (arcConfig.shouldOnlyKeepAppBeans()) {
            builder.addRemovalExclusion(new AbstractCompositeApplicationClassesPredicate<BeanInfo>(
                    applicationClassesIndex, generatedClassNames, applicationClassPredicates) {
                @Override
                protected DotName getDotName(BeanInfo bean) {
                    return bean.getBeanClass();
                }
            });
        }
        builder.addRemovalExclusion(new BeanClassNameExclusion(LifecycleEventRunner.class.getName()));
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
        for (UnremovableBeanBuildItem exclusion : removalExclusions) {
            builder.addRemovalExclusion(exclusion.getPredicate());
        }
        if (testClassPredicate.isPresent()) {
            builder.addRemovalExclusion(new Predicate<BeanInfo>() {
                @Override
                public boolean test(BeanInfo bean) {
                    return testClassPredicate.get().getPredicate().test(bean.getBeanClass().toString());
                }
            });
        }

        BeanProcessor beanProcessor = builder.build();
        ContextRegistrar.RegistrationContext context = beanProcessor.registerCustomContexts();
        return new ContextRegistrationPhaseBuildItem(context, beanProcessor);
    }

    // PHASE 2 - register all beans
    @BuildStep
    public BeanRegistrationPhaseBuildItem registerBeans(ContextRegistrationPhaseBuildItem contextRegistrationPhase,
            List<ContextConfiguratorBuildItem> contextConfigurators) {

        for (ContextConfiguratorBuildItem contextConfigurator : contextConfigurators) {
            for (ContextConfigurator value : contextConfigurator.getValues()) {
                // Just make sure the configurator is processed
                value.done();
            }
        }

        return new BeanRegistrationPhaseBuildItem(contextRegistrationPhase.getBeanProcessor().registerBeans(),
                contextRegistrationPhase.getBeanProcessor());
    }

    // PHASE 3 - initialize and validate the bean deployment
    @BuildStep
    public ValidationPhaseBuildItem validate(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            List<BeanConfiguratorBuildItem> beanConfigurators) {

        for (BeanConfiguratorBuildItem beanConfigurator : beanConfigurators) {
            for (BeanConfigurator<?> value : beanConfigurator.getValues()) {
                // Just make sure the configurator is processed
                value.done();
            }
        }

        beanRegistrationPhase.getBeanProcessor().initialize();
        return new ValidationPhaseBuildItem(beanRegistrationPhase.getBeanProcessor().validate(),
                beanRegistrationPhase.getBeanProcessor());
    }

    // PHASE 4 - generate resources and initialize the container
    @BuildStep(providesCapabilities = Capabilities.CDI_ARC, applicationArchiveMarkers = { "META-INF/beans.xml",
            "META-INF/services/javax.enterprise.inject.spi.Extension" })
    @Record(STATIC_INIT)
    public BeanContainerBuildItem generateResources(ArcRecorder recorder, ShutdownContextBuildItem shutdown,
            ValidationPhaseBuildItem validationPhase,
            List<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<GeneratedResourceBuildItem> generatedResource) throws Exception {

        for (ValidationErrorBuildItem validationError : validationErrors) {
            for (Throwable error : validationError.getValues()) {
                validationPhase.getContext().addDeploymentProblem(error);
            }
        }

        BeanProcessor beanProcessor = validationPhase.getBeanProcessor();
        beanProcessor.processValidationErrors(validationPhase.getContext());

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
        });
        for (ResourceOutput.Resource resource : resources) {
            switch (resource.getType()) {
                case JAVA_CLASS:
                    LOGGER.debugf("Add %s class: %s", (resource.isApplicationClass() ? "APP" : "FWK"),
                            resource.getFullyQualifiedName());
                    generatedClass.produce(new GeneratedClassBuildItem(resource.isApplicationClass(), resource.getName(),
                            resource.getData()));
                    break;
                case SERVICE_PROVIDER:
                    generatedResource.produce(
                            new GeneratedResourceBuildItem("META-INF/services/" + resource.getName(), resource.getData()));
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
                        .collect(Collectors.toList()),
                beanProcessor.getBeanDeployment().getRemovedBeans().stream().flatMap(b -> b.getTypes().stream())
                        .map(t -> t.name().toString())
                        .collect(Collectors.toSet()));

        return new BeanContainerBuildItem(beanContainer);

    }

    private abstract static class AbstractCompositeApplicationClassesPredicate<T> implements Predicate<T> {

        private final IndexView applicationClassesIndex;
        private final Set<DotName> generatedClassNames;
        private final List<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems;

        protected abstract DotName getDotName(T t);

        private AbstractCompositeApplicationClassesPredicate(IndexView applicationClassesIndex,
                Set<DotName> generatedClassNames,
                List<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems) {
            this.applicationClassesIndex = applicationClassesIndex;
            this.generatedClassNames = generatedClassNames;
            this.applicationClassPredicateBuildItems = applicationClassPredicateBuildItems;
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
            if (!applicationClassPredicateBuildItems.isEmpty()) {
                String className = dotName.toString();
                for (ApplicationClassPredicateBuildItem predicate : applicationClassPredicateBuildItems) {
                    if (predicate.test(className)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}