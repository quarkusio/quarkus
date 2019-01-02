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

import static org.jboss.shamrock.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.processor.BeanDefiningAnnotation;
import org.jboss.protean.arc.processor.BeanProcessor;
import org.jboss.protean.arc.processor.BeanProcessor.Builder;
import org.jboss.protean.arc.processor.ReflectionRegistration;
import org.jboss.protean.arc.processor.ResourceOutput;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.runtime.ArcDeploymentTemplate;
import org.jboss.shamrock.arc.runtime.LifecycleEventRunner;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanArchiveIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;
import org.jboss.shamrock.deployment.builditem.ServiceStartBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanDefiningAnnotationBuildItem;
import org.jboss.shamrock.deployment.cdi.GeneratedBeanBuildItem;
import org.jboss.shamrock.deployment.cdi.ResourceAnnotationBuildItem;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.undertow.ServletExtensionBuildItem;

public class ArcAnnotationProcessor {

    private static final DotName JAVA_LANG_OBJECT = DotName.createSimple(Object.class.getName());

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
    
    @BuildStep(providesCapabilities = Capabilities.CDI_ARC, applicationArchiveMarkers = { "META-INF/beans.xml",
            "META-INF/services/javax.enterprise.inject.spi.Extension" })
    @Record(STATIC_INIT)
    public BeanContainerBuildItem build(ArcDeploymentTemplate arcTemplate, BuildProducer<ServletExtensionBuildItem> extensions,
            BuildProducer<InjectionProviderBuildItem> injectionProvider, List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
                                        ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<GeneratedBeanBuildItem> generatedBeans, List<AnnotationsTransformerBuildItem> annotationTransformers, ShutdownContextBuildItem shutdown) throws Exception {


        List<String> additionalBeans = new ArrayList<>();
        for (AdditionalBeanBuildItem i : this.additionalBeans) {
            additionalBeans.addAll(i.getBeanNames());
        }
        additionalBeans.add(LifecycleEventRunner.class.getName());

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Observes.class.getName())); // graal bug

        // Index bean classes registered by shamrock
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        for (String beanClass : additionalBeans) {
            indexBeanClass(beanClass, indexer, beanArchiveIndex.getIndex(), additionalIndex);
        }
        Set<DotName> generatedClassNames = new HashSet<>();
        for (GeneratedBeanBuildItem beanClass : generatedBeans) {
            indexBeanClass(beanClass.getName(), indexer, beanArchiveIndex.getIndex(), additionalIndex, beanClass.getData());
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
        builder.setIndex(index);
        builder.setAdditionalBeanDefiningAnnotations(additionalBeanDefiningAnnotations.stream()
                .map((s) -> new BeanDefiningAnnotation(s.getName(), s.getDefaultScope()))
                .collect(Collectors.toList()));
        builder.setSharedAnnotationLiterals(false);
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
        BeanProcessor beanProcessor = builder.build();
        beanProcessor.process();

        ArcContainer container = arcTemplate.getContainer(shutdown);
        BeanContainer bc = arcTemplate.initBeanContainer(container,
                beanContainerListenerBuildItems.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener).collect(Collectors.toList()));
        injectionProvider.produce(new InjectionProviderBuildItem(arcTemplate.setupInjection(container)));
        extensions.produce(new ServletExtensionBuildItem(arcTemplate.setupRequestScope(container)));

        return new BeanContainerBuildItem(bc);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void startupEvent(ArcDeploymentTemplate template, List<ServiceStartBuildItem> startList, BeanContainerBuildItem beanContainer,
            ShutdownContextBuildItem shutdown) {
        template.handleLifecycleEvents(shutdown, beanContainer.getValue());
    }

    private void indexBeanClass(String beanClass, Indexer indexer, IndexView shamrockIndex, Set<DotName> additionalIndex) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = shamrockIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            log.debugf("Index bean class: %s", beanClass);
            try (InputStream stream = ArcAnnotationProcessor.class.getClassLoader().getResourceAsStream(beanClass.replace('.', '/') + ".class")) {
                beanInfo = indexer.index(stream);
                additionalIndex.add(beanInfo.name());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + beanClass);
            }
        } else {
            // The class could be indexed by shamrock - we still need to distinguish framework classes
            additionalIndex.add(beanClassName);
        }
        for (DotName annotationName : beanInfo.annotations().keySet()) {
            if (!additionalIndex.contains(annotationName) && shamrockIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = ArcAnnotationProcessor.class.getClassLoader()
                        .getResourceAsStream(annotationName.toString().replace('.', '/') + ".class")) {
                    log.debugf("Index annotation: %s", annotationName);
                    indexer.index(annotationStream);
                    additionalIndex.add(annotationName);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass);
                }
            }
        }
        if (!beanInfo.superName().equals(JAVA_LANG_OBJECT)) {
            indexBeanClass(beanInfo.superName().toString(), indexer, shamrockIndex, additionalIndex);
        }

    }

    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/beans.xml");
    }

    private void indexBeanClass(String beanClass, Indexer indexer, IndexView shamrockIndex, Set<DotName> additionalIndex, byte[] beanData) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = shamrockIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            log.infof("Index bean class: %s", beanClass);
            try (InputStream stream = new ByteArrayInputStream(beanData)) {
                beanInfo = indexer.index(stream);
                additionalIndex.add(beanInfo.name());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + beanClass);
            }
        } else {
            // The class could be indexed by shamrock - we still need to distinguish framework classes
            additionalIndex.add(beanClassName);
        }
        for (DotName annotationName : beanInfo.annotations().keySet()) {
            if (!additionalIndex.contains(annotationName) && shamrockIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = ArcAnnotationProcessor.class.getClassLoader()
                        .getResourceAsStream(annotationName.toString().replace('.', '/') + ".class")) {
                    log.infof("Index annotation: %s", annotationName);
                    indexer.index(annotationStream);
                    additionalIndex.add(annotationName);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass);
                }
            }
        }
    }
}
