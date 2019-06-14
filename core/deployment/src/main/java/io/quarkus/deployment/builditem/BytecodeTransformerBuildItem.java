package io.quarkus.deployment.builditem;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.builder.item.MultiBuildItem;

public final class BytecodeTransformerBuildItem extends MultiBuildItem {

    final String classToTransform;
    final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;

    public BytecodeTransformerBuildItem(String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
        this.classToTransform = classToTransform;
        this.visitorFunction = visitorFunction;
    }

    public String getClassToTransform() {
        return classToTransform;
    }

    public BiFunction<String, ClassVisitor, ClassVisitor> getVisitorFunction() {
        return visitorFunction;
    }
}
