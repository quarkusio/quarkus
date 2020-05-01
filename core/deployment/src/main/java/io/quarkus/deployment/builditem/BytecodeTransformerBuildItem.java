package io.quarkus.deployment.builditem;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.builder.item.MultiBuildItem;

public final class BytecodeTransformerBuildItem extends MultiBuildItem {

    /**
     * If this is true it means the class should be loaded eagerly by a thread pool in dev mode
     * on multi threaded systems.
     *
     * Transformation is expensive, so doing it this way can speed up boot time.
     */
    final boolean eager;
    final String classToTransform;
    final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;

    public BytecodeTransformerBuildItem(String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
        this(false, classToTransform, visitorFunction);
    }

    public BytecodeTransformerBuildItem(boolean eager, String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
        this.eager = eager;
        this.classToTransform = classToTransform;
        this.visitorFunction = visitorFunction;
    }

    public String getClassToTransform() {
        return classToTransform;
    }

    public BiFunction<String, ClassVisitor, ClassVisitor> getVisitorFunction() {
        return visitorFunction;
    }

    public boolean isEager() {
        return eager;
    }
}
