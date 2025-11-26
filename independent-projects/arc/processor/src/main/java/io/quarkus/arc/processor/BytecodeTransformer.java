package io.quarkus.arc.processor;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;

public class BytecodeTransformer {

    public static BytecodeTransformer forInputTransformer(String classToTransform,
            BiFunction<String, byte[], byte[]> inputTransformer) {
        return new BytecodeTransformer(classToTransform, null, inputTransformer);
    }

    private final String classToTransform;
    private final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;
    private final BiFunction<String, byte[], byte[]> inputTransformer;

    public BytecodeTransformer(String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
        this(classToTransform, visitorFunction, null);
    }

    private BytecodeTransformer(String classToTransform, BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction,
            BiFunction<String, byte[], byte[]> inputTransformer) {
        super();
        this.classToTransform = classToTransform;
        this.visitorFunction = visitorFunction;
        this.inputTransformer = inputTransformer;
    }

    public String getClassToTransform() {
        return classToTransform;
    }

    public BiFunction<String, ClassVisitor, ClassVisitor> getVisitorFunction() {
        return visitorFunction;
    }

    public BiFunction<String, byte[], byte[]> getInputTransformer() {
        return inputTransformer;
    }

}
