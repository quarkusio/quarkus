package org.jboss.shamrock.deployment.builditem;

import java.util.function.BiFunction;

import org.jboss.builder.item.MultiBuildItem;
import org.objectweb.asm.ClassVisitor;

public final class BytecodeTransformerBuildItem extends MultiBuildItem {

    final String classToTransform;
    final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;

    public BytecodeTransformerBuildItem(String classToTransform, BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
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
