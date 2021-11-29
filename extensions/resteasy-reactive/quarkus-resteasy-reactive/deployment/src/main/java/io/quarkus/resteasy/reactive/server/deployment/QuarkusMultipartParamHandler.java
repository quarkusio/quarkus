package io.quarkus.resteasy.reactive.server.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.generation.multipart.MultipartPopulatorGenerator;
import org.jboss.resteasy.reactive.server.processor.generation.multipart.MultipartTransformer;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class QuarkusMultipartParamHandler implements EndpointIndexer.MultipartParameterIndexerExtension {
    private final Map<String, String> multipartInputGeneratedPopulators = new HashMap<>();
    final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    final Predicate<String> applicationClassPredicate;
    final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;
    final BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer;

    public QuarkusMultipartParamHandler(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            Predicate<String> applicationClassPredicate, BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer) {
        this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
        this.applicationClassPredicate = applicationClassPredicate;
        this.reflectiveClassProducer = reflectiveClassProducer;
        this.bytecodeTransformerBuildProducer = bytecodeTransformerBuildProducer;
    }

    @Override
    public void handleMultipartParameter(ClassInfo multipartClassInfo, IndexView index) {
        String className = multipartClassInfo.name().toString();
        if (multipartInputGeneratedPopulators.containsKey(className)) {
            // we've already seen this class before and have done all we need to make it work
            return;
        }
        reflectiveClassProducer.produce(new ReflectiveClassBuildItem(false, false, className));
        String populatorClassName = MultipartPopulatorGenerator.generate(multipartClassInfo,
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, applicationClassPredicate.test(className)),
                index);
        multipartInputGeneratedPopulators.put(className, populatorClassName);

        // transform the multipart pojo (and any super-classes) so we can access its fields no matter what
        ClassInfo currentClassInHierarchy = multipartClassInfo;
        while (true) {
            bytecodeTransformerBuildProducer
                    .produce(new BytecodeTransformerBuildItem(currentClassInHierarchy.name().toString(),
                            new MultipartTransformer(populatorClassName)));

            DotName superClassDotName = currentClassInHierarchy.superName();
            if (superClassDotName.equals(DotNames.OBJECT_NAME)) {
                break;
            }
            ClassInfo newCurrentClassInHierarchy = index.getClassByName(superClassDotName);
            if (newCurrentClassInHierarchy == null) {
                break;
            }
            currentClassInHierarchy = newCurrentClassInHierarchy;
        }

    }
}
