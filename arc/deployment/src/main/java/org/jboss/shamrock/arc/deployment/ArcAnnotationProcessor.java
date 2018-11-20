package org.jboss.shamrock.arc.deployment;

import static org.jboss.shamrock.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.ActivateRequestContextInterceptor;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.processor.AnnotationsTransformer;
import org.jboss.protean.arc.processor.BeanProcessor;
import org.jboss.protean.arc.processor.BeanProcessor.Builder;
import org.jboss.protean.arc.processor.ReflectionRegistration;
import org.jboss.protean.arc.processor.ResourceOutput;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.runtime.ArcDeploymentTemplate;
import org.jboss.shamrock.arc.runtime.StartupEventRunner;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanArchiveIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.InjectionProviderBuildItem;
import org.jboss.shamrock.deployment.builditem.ServiceStartBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.cdi.AnnotationTransformerBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.cdi.GeneratedBeanBuildItem;
import org.jboss.shamrock.deployment.cdi.ResourceAnnotationBuildItem;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.undertow.ServletExtensionBuildItem;

import io.smallrye.config.inject.ConfigProducer;

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
    List<ResourceAnnotationBuildItem> resourceAnnotations;


    @BuildStep(providesCapabilities = Capabilities.CDI_ARC, applicationArchiveMarkers = {"META-INF/beans.xml", "META-INF/services/javax.enterprise.inject.spi.Extension"})
    @Record(STATIC_INIT)
    public BeanContainerBuildItem build(ArcDeploymentTemplate arcTemplate, BuildProducer<ServletExtensionBuildItem> extensions, BuildProducer<InjectionProviderBuildItem> injectionProvider,
                                        List<BeanContainerListenerBuildItem> beanContainerListenerBuildItems,
                                        List<GeneratedBeanBuildItem> generatedBeans,
                                        List<AnnotationTransformerBuildItem> annotationTransformers,
                                        ShutdownContextBuildItem shutdown) throws Exception {

        List<String> additionalBeans = new ArrayList<>();
        for (AdditionalBeanBuildItem i : this.additionalBeans) {
            additionalBeans.addAll(i.getBeanNames());
        }
        additionalBeans.add(StartupEventRunner.class.getName());

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Observes.class.getName())); // graal bug

        List<DotName> additionalBeanDefiningAnnotations = new ArrayList<>();
        additionalBeanDefiningAnnotations.add(DotName.createSimple("javax.servlet.annotation.WebServlet"));
        additionalBeanDefiningAnnotations.add(DotName.createSimple("javax.ws.rs.Path"));

        // TODO MP config
        additionalBeans.add(ConfigProducer.class.getName());

        // Index bean classes registered by shamrock
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        for (String beanClass : additionalBeans) {
            indexBeanClass(beanClass, indexer, beanArchiveIndex.getIndex(), additionalIndex);
        }
        Set<String> frameworkPackages = additionalIndex.stream().map(dotName -> {
            String name = dotName.toString();
            return name.substring(0, name.lastIndexOf("."));
        }).collect(Collectors.toSet());
        List<Predicate<String>> frameworkPredicates = new ArrayList<>();
        frameworkPredicates.add(fqcn -> {
            for (String frameworkPackage : frameworkPackages) {
                if (fqcn.startsWith(frameworkPackage)) {
                    return true;
                }
            }
            return false;
        });
        // For some odd reason we cannot add org.jboss.protean.arc as a fwk package
        frameworkPredicates.add(fqcn -> {
            return fqcn.startsWith(ActivateRequestContextInterceptor.class.getName()) || fqcn.startsWith("org.jboss.protean.arc.ActivateRequestContext");
        });

        for (GeneratedBeanBuildItem beanClass : generatedBeans) {
            indexBeanClass(beanClass.getName(), indexer, beanArchiveIndex.getIndex(), additionalIndex, beanClass.getData());
        }
        CompositeIndex index = CompositeIndex.create(indexer.complete(), beanArchiveIndex.getIndex());
        Builder builder = BeanProcessor.builder();
        builder.setIndex(index);
        builder.setAdditionalBeanDefiningAnnotations(additionalBeanDefiningAnnotations);
        builder.setSharedAnnotationLiterals(false);
        builder.addResourceAnnotations(resourceAnnotations.stream().map(ResourceAnnotationBuildItem::getName).collect(Collectors.toList()));
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
        for (AnnotationTransformerBuildItem transformer : annotationTransformers) {
            // TODO make use of Arc API instead of BiFunction
            builder.addAnnotationTransformer(new AnnotationsTransformer() {

                @Override
                public Collection<AnnotationInstance> transform(AnnotationTarget target, Collection<AnnotationInstance> annotations) {
                    return transformer.getTransformer().apply(target, annotations);
                }
            });
        }

        builder.setOutput(new ResourceOutput() {
            @Override
            public void writeResource(Resource resource) throws IOException {
                switch (resource.getType()) {
                    case JAVA_CLASS:
                        // TODO a better way to identify app classes
                        boolean isAppClass = true;

                        if (!resource.getFullyQualifiedName().contains("$$APP$$")) {
                            // ^ horrible hack, we really need to look into into
                            // app vs framework classes cause big problems for the runtime runner
                            for (Predicate<String> predicate : frameworkPredicates) {
                                if (predicate.test(resource.getFullyQualifiedName())) {
                                    isAppClass = false;
                                    break;
                                }
                            }
                        }
                        log.debugf("Add %s class: %s", (isAppClass ? "APP" : "FWK"), resource.getFullyQualifiedName());
                        generatedClass.produce(new GeneratedClassBuildItem(isAppClass, resource.getName(), resource.getData()));
                        break;
                    case SERVICE_PROVIDER:
                        generatedResource.produce(new GeneratedResourceBuildItem("META-INF/services/" + resource.getName(), resource.getData()));
                    default:
                        break;
                }
            }
        });
        for (BeanRegistrarBuildItem i : beanRegistrars) {
            builder.addBeanRegistrar(i.getBeanRegistrar());
        }
        BeanProcessor beanProcessor = builder.build();
        beanProcessor.process();

        ArcContainer container = arcTemplate.getContainer(shutdown);
        BeanContainer bc = arcTemplate.initBeanContainer(container, beanContainerListenerBuildItems.stream().map(BeanContainerListenerBuildItem::getBeanContainerListener).collect(Collectors.toList()));
        injectionProvider.produce(new InjectionProviderBuildItem(arcTemplate.setupInjection(container)));
        extensions.produce(new ServletExtensionBuildItem(arcTemplate.setupRequestScope(container)));

        return new BeanContainerBuildItem(bc);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void startupEvent(ArcDeploymentTemplate template, List<ServiceStartBuildItem> startList, BeanContainerBuildItem beanContainer) {
        template.fireStartupEvent(beanContainer.getValue());
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
