package org.jboss.resteasy.reactive.server.processor.generation.multipart;

import io.quarkus.gizmo.ClassOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.objectweb.asm.ClassVisitor;

public class GeneratedMultipartParamIndexerExtension implements EndpointIndexer.MultipartParameterIndexerExtension {
    private final Map<String, String> multipartInputGeneratedPopulators = new HashMap<>();
    final BiConsumer<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformations;
    final ClassOutput classOutput;

    public GeneratedMultipartParamIndexerExtension(Map<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformations,
            ClassOutput classOutput) {
        this.transformations = transformations::put;
        this.classOutput = classOutput;
    }

    public GeneratedMultipartParamIndexerExtension(
            BiConsumer<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformations,
            ClassOutput classOutput) {
        this.transformations = transformations;
        this.classOutput = classOutput;
    }

    @Override
    public void handleMultipartParameter(ClassInfo multipartClassInfo, IndexView index) {
        String className = multipartClassInfo.name().toString();
        if (multipartInputGeneratedPopulators.containsKey(className)) {
            // we've already seen this class before and have done all we need to make it work
            return;
        }
        String populatorClassName = MultipartPopulatorGenerator.generate(multipartClassInfo, classOutput, index);
        multipartInputGeneratedPopulators.put(className, populatorClassName);

        // transform the multipart pojo (and any super-classes) so we can access its fields no matter what
        ClassInfo currentClassInHierarchy = multipartClassInfo;
        while (true) {
            transformations.accept(currentClassInHierarchy.name().toString(), new MultipartTransformer(populatorClassName));

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
