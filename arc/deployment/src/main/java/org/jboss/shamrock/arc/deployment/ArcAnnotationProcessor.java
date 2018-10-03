package org.jboss.shamrock.arc.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
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
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.processor.BeanProcessor;
import org.jboss.protean.arc.processor.BeanProcessor.Builder;
import org.jboss.protean.arc.processor.ReflectionRegistration;
import org.jboss.protean.arc.processor.ResourceOutput;
import org.jboss.shamrock.arc.runtime.ArcDeploymentTemplate;
import org.jboss.shamrock.arc.runtime.StartupEventRunner;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanArchiveIndex;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;

import io.smallrye.config.inject.ConfigProducer;

public class ArcAnnotationProcessor implements ResourceProcessor {

    private static final DotName JAVA_LANG_OBJECT = DotName.createSimple(Object.class.getName());
    private static final Logger log = Logger.getLogger("org.jboss.shamrock.arc.deployment.processor");

    @Inject
    BeanDeployment beanDeployment;

    @Inject
    BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        beanDeployment.addAdditionalBean(StartupEventRunner.class);

        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.ARC_DEPLOYMENT)) {

            ArcDeploymentTemplate template = recorder.getRecordingProxy(ArcDeploymentTemplate.class);
            processorContext.addReflectiveClass(true, false, Observes.class.getName()); //graal bug

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
            for (BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>> transformer : beanDeployment
                    .getAnnotationTransformers()) {
                builder.addAnnotationTransformer(transformer);
            }

            builder.setOutput(new ResourceOutput() {
                @Override
                public void writeResource(Resource resource) throws IOException {
                    switch (resource.getType()) {
                        case JAVA_CLASS:
                            // TODO a better way to identify app classes
                            boolean isAppClass = true;

                            if(!resource.getFullyQualifiedName().contains("$$APP$$")) {
                                //horrible hack, we really need to look into into
                                //app vs framework classes cause big problems for the runtime runner
                                for (String frameworkPackage : frameworkPackages) {
                                    if (resource.getFullyQualifiedName().startsWith(frameworkPackage)) {
                                        isAppClass = false;
                                    }
                                }
                            }
                            log.infof("Add %s class: %s", (isAppClass ? "APP" : "FWK"), resource.getFullyQualifiedName());
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

            ArcContainer container = template.getContainer(null);
            template.initBeanContainer(container);
            template.setupInjection(null, container);
            template.setupRequestScope(null, null);
        }

        try (BytecodeRecorder recorder = processorContext.addDeploymentTask(RuntimePriority.STARTUP_EVENT)) {
            recorder.getRecordingProxy(ArcDeploymentTemplate.class).fireStartupEvent(null);
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
            log.infof("Index bean class: %s", beanClass);
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
                    log.infof("Index annotation: %s", annotationName);
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
