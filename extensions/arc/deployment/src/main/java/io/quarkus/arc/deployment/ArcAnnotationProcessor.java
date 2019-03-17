/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import io.quarkus.arc.processor.BeanProcessor.Builder;
import io.quarkus.arc.processor.ReflectionRegistration;
import io.quarkus.arc.processor.ResourceOutput;
import io.quarkus.arc.runtime.AdditionalBean;
import io.quarkus.arc.runtime.ArcDeploymentTemplate;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.LifecycleEventRunner;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
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
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    List<AdditionalBeanBuildItem> additionalBeans;

    @Inject
    BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods;

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

    /**
     * The configuration for ArC, the CDI-based injection facility.
     */
    ArcConfig arc;

    @BuildStep(providesCapabilities = Capabilities.CDI_ARC, applicationArchiveMarkers = { "META-INF/beans.xml",
            "META-INF/services/javax.enterprise.inject.spi.Extension" })
    @Record(STATIC_INIT)
    public BeanContainerBuildItem build(ArcDeploymentTemplate arcTemplate,
            List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<AnnotationsTransformerBuildItem> annotationTransformers,
            ShutdownContextBuildItem shutdown, List<AdditionalStereotypeBuildItem> additionalStereotypeBuildItems,
            BuildProducer<FeatureBuildItem> feature)
            throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CDI));

        List<String> additionalBeans = beanArchiveIndex.getAdditionalBeans();
        Set<DotName> generatedClassNames = beanArchiveIndex.getGeneratedClassNames();
        IndexView index = beanArchiveIndex.getIndex();
        Builder builder = BeanProcessor.builder();
        IndexView applicationClassesIndex = applicationArchivesBuildItem.getRootArchive().getIndex();
        builder.setApplicationClassPredicate(new AbstractApplicationClassesPredicate<DotName>(
                applicationClassesIndex, generatedClassNames) {
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
                ClassInfo target = transformationContext.getTarget().asClass();
                if (additionalBeans.contains(target.name().toString()) && !target.annotations().containsKey(ADDITIONAL_BEAN)) {
                    // Add special stereotype so that @Dependent is automatically used even if no scope is declared
                    transformationContext.transform().add(ADDITIONAL_BEAN).done();
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
        for (AnnotationsTransformerBuildItem transformerItem : annotationTransformers) {
            builder.addAnnotationTransformer(transformerItem.getAnnotationsTransformer());
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
        builder.setRemoveUnusedBeans(arc.removeUnusedBeans);
        if (arc.keepApplicationBeans) {
            builder.addRemovalExclusion(new AbstractApplicationClassesPredicate<BeanInfo>(
                    applicationClassesIndex, generatedClassNames) {
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

        BeanProcessor beanProcessor = builder.build();
        BeanDeployment beanDeployment = beanProcessor.process();

        ArcContainer container = arcTemplate.getContainer(shutdown);
        BeanContainer beanContainer = arcTemplate.initBeanContainer(container,
                beanContainerListenerBuildItems.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener)
                        .collect(Collectors.toList()),
                beanDeployment.getRemovedBeans().stream().flatMap(b -> b.getTypes().stream()).map(t -> t.name().toString())
                        .collect(Collectors.toSet()));

        return new BeanContainerBuildItem(beanContainer);
    }

    private abstract static class AbstractApplicationClassesPredicate<T> implements Predicate<T> {

        private final IndexView applicationClassesIndex;
        private final Set<DotName> generatedClasses;

        protected abstract DotName getDotName(T t);

        protected AbstractApplicationClassesPredicate(IndexView applicationClassesIndex, Set<DotName> generatedClasses) {
            this.applicationClassesIndex = applicationClassesIndex;
            this.generatedClasses = generatedClasses;
        }

        @Override
        public boolean test(T t) {
            final DotName dotName = getDotName(t);
            if (applicationClassesIndex.getClassByName(dotName) != null) {
                return true;
            }
            return generatedClasses.contains(dotName);
        }
    }
}
