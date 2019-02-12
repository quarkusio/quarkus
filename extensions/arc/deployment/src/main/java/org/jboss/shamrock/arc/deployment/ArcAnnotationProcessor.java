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

package org.jboss.shamrock.arc.deployment;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.processor.AnnotationsTransformer;
import org.jboss.protean.arc.processor.BeanDefiningAnnotation;
import org.jboss.protean.arc.processor.BeanDeployment;
import org.jboss.protean.arc.processor.BeanProcessor;
import org.jboss.protean.arc.processor.BeanProcessor.Builder;
import org.jboss.protean.arc.processor.ReflectionRegistration;
import org.jboss.protean.arc.processor.ResourceOutput;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem.BeanClassNameExclusion;
import org.jboss.shamrock.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import org.jboss.shamrock.arc.runtime.ArcDeploymentTemplate;
import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.arc.runtime.LifecycleEventRunner;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.index.IndexingUtil;

public class ArcAnnotationProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.shamrock.arc.deployment.processor");

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
            BuildProducer<InjectionProviderBuildItem> injectionProvider, List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
            ApplicationArchivesBuildItem applicationArchivesBuildItem, List<GeneratedBeanBuildItem> generatedBeans,
            List<AnnotationsTransformerBuildItem> annotationTransformers, ShutdownContextBuildItem shutdown, BuildProducer<FeatureBuildItem> feature)
            throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CDI));

        List<String> additionalBeans = new ArrayList<>();
        for (AdditionalBeanBuildItem i : this.additionalBeans) {
            additionalBeans.addAll(i.getBeanClasses());
        }
        additionalBeans.add(LifecycleEventRunner.class.getName());

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Observes.class.getName())); // graal bug

        // Index bean classes registered by shamrock
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        for (String beanClass : additionalBeans) {
            IndexingUtil.indexClass(beanClass, indexer, beanArchiveIndex.getIndex(), additionalIndex, 
                                    ArcAnnotationProcessor.class.getClassLoader());
        }
        Set<DotName> generatedClassNames = new HashSet<>();
        for (GeneratedBeanBuildItem beanClass : generatedBeans) {
            IndexingUtil.indexClass(beanClass.getName(), indexer, beanArchiveIndex.getIndex(), additionalIndex, 
                                    ArcAnnotationProcessor.class.getClassLoader(), beanClass.getData());
            generatedClassNames.add(DotName.createSimple(beanClass.getName()));
        }

        CompositeIndex index = CompositeIndex.create(indexer.complete(), beanArchiveIndex.getIndex());
        Builder builder = BeanProcessor.builder();
        builder.setApplicationClassPredicate(new Predicate<DotName>() {
            @Override
            public boolean test(DotName dotName) {
                if(applicationArchivesBuildItem.getRootArchive().getIndex().getClassByName(dotName) != null) {
                    return true;
                }
                if(generatedClassNames.contains(dotName)) {
                    return true;
                }
                return false;
            }
        });
        builder.addAnnotationTransformer(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return AnnotationTarget.Kind.CLASS == kind;
            }
            @Override
            public void transform(TransformationContext transformationContext) {
                if (additionalBeans.contains(transformationContext.getTarget().asClass().name().toString())) {
                    transformationContext.transform().add(Dependent.class).done();
                }
            }
        });
        builder.setIndex(index);

        builder.setAdditionalBeanDefiningAnnotations(additionalBeanDefiningAnnotations.stream()
                .map((s) -> new BeanDefiningAnnotation(s.getName(), s.getDefaultScope()))
                .collect(Collectors.toList()));
        builder.setSharedAnnotationLiterals(true);
        builder.addResourceAnnotations(resourceAnnotations.stream()
                .map(ResourceAnnotationBuildItem::getName)
                .collect(Collectors.toList()));
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
                        log.debugf("Add %s class: %s", (resource.isApplicationClass() ? "APP" : "FWK"), resource.getFullyQualifiedName());
                        generatedClass.produce(new GeneratedClassBuildItem(resource.isApplicationClass(), resource.getName(), resource.getData()));
                        break;
                    case SERVICE_PROVIDER:
                        generatedResource.produce(new GeneratedResourceBuildItem("META-INF/services/" + resource.getName(), resource.getData()));
                    default:
                        break;
                }
            }
        });
        for (BeanRegistrarBuildItem item : beanRegistrars) {
            builder.addBeanRegistrar(item.getBeanRegistrar());
        }
        for (BeanDeploymentValidatorBuildItem item : beanDeploymentValidators) {
            builder.addBeanDeploymentValidator(item.getBeanDeploymentValidator());
        }
        builder.setRemoveUnusedBeans(arc.removeUnusedBeans);
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
        BeanContainer beanContainer =
            arcTemplate.initBeanContainer(
                container,
                beanContainerListenerBuildItems
                    .stream()
                    .map(BeanContainerListenerBuildItem::getBeanContainerListener)
                    .collect(Collectors.toList()),
                beanDeployment
                    .getRemovedBeans()
                    .stream()
                    .flatMap(b -> b.getTypes().stream())
                    .map(t -> t.name().toString())
                    .collect(Collectors.toSet()));
            injectionProvider.produce(new InjectionProviderBuildItem(arcTemplate.setupInjection(container)));

        return new BeanContainerBuildItem(beanContainer);
    }
}
