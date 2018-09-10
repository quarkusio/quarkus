package org.jboss.shamrock.arc.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.processor.BeanProcessor;
import org.jboss.protean.arc.processor.BeanProcessor.Builder;
import org.jboss.protean.arc.processor.ReflectionRegistration;
import org.jboss.protean.arc.processor.ResourceOutput;
import org.jboss.shamrock.arc.runtime.ArcDeploymentTemplate;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanArchiveIndex;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;

import io.smallrye.config.inject.ConfigProducer;

public class ArcAnnotationProcessor implements ResourceProcessor {

    private static final DotName INJECT = DotName.createSimple("javax.inject.Inject");
    @Inject
    BeanDeployment beanDeployment;

    @Inject
    BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.ARC_DEPLOYMENT)) {

            ArcDeploymentTemplate template = recorder.getRecordingProxy(ArcDeploymentTemplate.class);

            List<DotName> additionalBeanDefiningAnnotations = new ArrayList<>();
            additionalBeanDefiningAnnotations.add(DotName.createSimple("javax.servlet.annotation.WebServlet"));
            additionalBeanDefiningAnnotations.add(DotName.createSimple("javax.ws.rs.Path"));

            // TODO MP config
            beanDeployment.addAdditionalBean(ConfigProducer.class);

            // Index bean classes registered by shamrock
            Indexer indexer = new Indexer();
            Set<DotName> additionalIndex = new HashSet<>();
            for (String beanClass : beanDeployment.getAdditionalBeans()) {
                indexBeanClass(beanClass, indexer, beanArchiveIndex.getIndex(), additionalIndex);
            }
            Set<String> frameworkPackages = additionalIndex.stream().map(dotName -> {
                String name = dotName.toString();
                return name.substring(0, name.lastIndexOf("."));
            }).collect(Collectors.toSet());

            for (Map.Entry<String, byte[]> beanClass : beanDeployment.getGeneratedBeans().entrySet()) {
                indexBeanClass(beanClass.getKey(), indexer, beanArchiveIndex.getIndex(), additionalIndex, beanClass.getValue());
            }
            CompositeIndex index = CompositeIndex.create(indexer.complete(), beanArchiveIndex.getIndex());
            Builder builder = BeanProcessor.builder();
            builder.setIndex(index);
            builder.setAdditionalBeanDefiningAnnotations(additionalBeanDefiningAnnotations);
            builder.setSharedAnnotationLiterals(false);
            builder.setReflectionRegistration(new ReflectionRegistration() {
                @Override
                public void registerMethod(MethodInfo methodInfo) {
                    processorContext.addReflectiveMethod(methodInfo);
                }

                @Override
                public void registerField(FieldInfo fieldInfo) {
                    processorContext.addReflectiveField(fieldInfo);
                }
            });
            builder.setOutput(new ResourceOutput() {
                @Override
                public void writeResource(Resource resource) throws IOException {
                    switch (resource.getType()) {
                        case JAVA_CLASS:
                            // TODO a better way to identify app classes
                            boolean isAppClass = true;
                            for (String frameworkPackage : frameworkPackages) {
                                if (resource.getFullyQualifiedName().startsWith(frameworkPackage)) {
                                    isAppClass = false;
                                }
                            }
                            System.out.println("Add " + (isAppClass ? "APP" : "FWK") + " class: " + resource.getFullyQualifiedName());
                            processorContext.addGeneratedClass(isAppClass, resource.getName(), resource.getData());
                            break;
                        case SERVICE_PROVIDER:
                            processorContext.createResource("META-INF/services/" + resource.getName(), resource.getData());
                        default:
                            break;
                    }
                }
            });
            BeanProcessor beanProcessor = builder.build();
            beanProcessor.process();

            ArcContainer container = template.getContainer();
            template.initBeanContainer(container);
            template.setupInjection(container);
        }
    }

    @Override
    public int getPriority() {
        return RuntimePriority.ARC_DEPLOYMENT;
    }

    private void indexBeanClass(String beanClass, Indexer indexer, IndexView shamrockIndex, Set<DotName> additionalIndex) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = shamrockIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            System.out.println("Index bean class: " + beanClass);
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
                    System.out.println("Index annotation: " + annotationName);
                    indexer.index(annotationStream);
                    additionalIndex.add(annotationName);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass);
                }
            }
        }
    }

    private void indexBeanClass(String beanClass, Indexer indexer, IndexView shamrockIndex, Set<DotName> additionalIndex, byte[] beanData) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = shamrockIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            System.out.println("Index bean class: " + beanClass);
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
                    System.out.println("Index annotation: " + annotationName);
                    indexer.index(annotationStream);
                    additionalIndex.add(annotationName);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass);
                }
            }
        }
    }
}
