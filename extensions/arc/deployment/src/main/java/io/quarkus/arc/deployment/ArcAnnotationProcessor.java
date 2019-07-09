package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanDefiningAnnotation;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BuiltinScope;
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

public class ArcAnnotationProcessor {

    private static final Logger log = Logger.getLogger("io.quarkus.arc.deployment.processor");

    static final DotName ADDITIONAL_BEAN = DotName.createSimple(AdditionalBean.class.getName());

    @Inject
    BeanArchiveIndexBuildItem beanArchiveIndex;

    @Inject
    BuildProducer<GeneratedClassBuildItem> generatedClass;

    @Inject
    BuildProducer<GeneratedResourceBuildItem> generatedResource;

    @Inject
    List<AdditionalBeanBuildItem> additionalBeans;

    @Inject
    BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    @Inject
    BuildProducer<ReflectiveFieldBuildItem> reflectiveFields;

    @Inject
    List<BeanRegistrarBuildItem> beanRegistrars;

    @Inject
    List<ContextRegistrarBuildItem> contextRegistrars;

    @Inject
    List<BeanDeploymentValidatorBuildItem> beanDeploymentValidators;

    @Inject
    List<ResourceAnnotationBuildItem> resourceAnnotations;

    @Inject
    List<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotations;

    @Inject
    List<UnremovableBeanBuildItem> removalExclusions;

    @Inject
    Optional<TestClassPredicateBuildItem> testClassPredicate;

    /**
     * The configuration for ArC, the CDI-based injection facility.
     */
    ArcConfig arc;

    @BuildStep(providesCapabilities = Capabilities.CDI_ARC, applicationArchiveMarkers = { "META-INF/beans.xml",
            "META-INF/services/javax.enterprise.inject.spi.Extension" })
    @Record(STATIC_INIT)
    public BeanContainerBuildItem build(ArcRecorder recorder,
            List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<AnnotationsTransformerBuildItem> annotationTransformers,
            List<InjectionPointTransformerBuildItem> injectionPointTransformers,
            ShutdownContextBuildItem shutdown, List<AdditionalStereotypeBuildItem> additionalStereotypeBuildItems,
            List<ApplicationClassPredicateBuildItem> applicationClassPredicates,
            BuildProducer<FeatureBuildItem> feature)
            throws Exception {

        if (!arc.isRemoveUnusedBeansFieldValid()) {
            throw new IllegalArgumentException("Invalid configuration value set for 'quarkus.arc.remove-unused-beans'." +
                    " Please use one of " + ArcConfig.ALLOWED_REMOVE_UNUSED_BEANS_VALUES);
        }

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CDI));

        List<String> additionalBeans = beanArchiveIndex.getAdditionalBeans();
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
                if (additionalBeans.contains(beanClassName)) {
                    if (BuiltinScope.isDeclaredOn(beanClass)) {
                        // If it declares a built-in scope no action is needed
                        return;
                    }
                    // Try to determine the default scope
                    DotName defaultScope = ArcAnnotationProcessor.this.additionalBeans.stream()
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
        builder.setReflectionRegistration(new ReflectionRegistration() {
            @Override
            public void registerMethod(MethodInfo methodInfo) {
                reflectiveMethods.produce(new ReflectiveMethodBuildItem(methodInfo));
            }

            @Override
            public void registerField(FieldInfo fieldInfo) {
                reflectiveFields.produce(new ReflectiveFieldBuildItem(fieldInfo));
            }
        });
        // register all annotation transformers
        for (AnnotationsTransformerBuildItem transformerItem : annotationTransformers) {
            builder.addAnnotationTransformer(transformerItem.getAnnotationsTransformer());
        }
        // register all injection point transformers
        for (InjectionPointTransformerBuildItem transformerItem : injectionPointTransformers) {
            builder.addInjectionPointTransformer(transformerItem.getInjectionPointsTransformer());
        }

        builder.setOutput(new ResourceOutput() {
            @Override
            public void writeResource(Resource resource) throws IOException {
                switch (resource.getType()) {
                    case JAVA_CLASS:
                        log.debugf("Add %s class: %s", (resource.isApplicationClass() ? "APP" : "FWK"),
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
        });
        for (BeanRegistrarBuildItem item : beanRegistrars) {
            builder.addBeanRegistrar(item.getBeanRegistrar());
        }
        for (ContextRegistrarBuildItem item : contextRegistrars) {
            builder.addContextRegistrar(item.getContextRegistrar());
        }
        for (BeanDeploymentValidatorBuildItem item : beanDeploymentValidators) {
            builder.addBeanDeploymentValidator(item.getBeanDeploymentValidator());
        }
        builder.setRemoveUnusedBeans(arc.shouldEnableBeanRemoval());
        if (arc.shouldOnlyKeepAppBeans()) {
            builder.addRemovalExclusion(new AbstractCompositeApplicationClassesPredicate<BeanInfo>(
                    applicationClassesIndex, generatedClassNames, applicationClassPredicates) {
                @Override
                protected DotName getDotName(BeanInfo bean) {
                    return bean.getBeanClass();
                }
            });
        }
        builder.addRemovalExclusion(new BeanClassNameExclusion(LifecycleEventRunner.class.getName()));
        for (AdditionalBeanBuildItem additionalBean : this.additionalBeans) {
            if (!additionalBean.isRemovable()) {
                for (String beanClass : additionalBean.getBeanClasses()) {
                    builder.addRemovalExclusion(new BeanClassNameExclusion(beanClass));
                }
            }
        }
        for (BeanDefiningAnnotationBuildItem annotation : this.additionalBeanDefiningAnnotations) {
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
        BeanDeployment beanDeployment = beanProcessor.process();

        // Register all qualifiers for reflection to support type-safe resolution at runtime in native image
        for (ClassInfo qualifier : beanDeployment.getQualifiers()) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, qualifier.name().toString()));
        }

        ArcContainer container = recorder.getContainer(shutdown);
        BeanContainer beanContainer = recorder.initBeanContainer(container,
                beanContainerListenerBuildItems.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener)
                        .collect(Collectors.toList()),
                beanDeployment.getRemovedBeans().stream().flatMap(b -> b.getTypes().stream()).map(t -> t.name().toString())
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
