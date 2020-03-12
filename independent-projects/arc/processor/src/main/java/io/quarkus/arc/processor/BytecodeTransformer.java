package io.quarkus.arc.processor;

import java.util.function.BiFunction;
import org.objectweb.asm.ClassVisitor;

public class BytecodeTransformer {

    final String classToTransform;
    final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;

    public BytecodeTransformer(String classToTransform,
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
