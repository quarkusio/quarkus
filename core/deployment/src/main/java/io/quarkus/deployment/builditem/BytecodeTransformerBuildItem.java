package io.quarkus.deployment.builditem;

import java.util.Set;
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

    /**
     * A set of class names that need to be present in the const pool for the transformation to happen. These
     * need to be in JVM internal format.
     *
     * The transformation is only applied if at least one of the entries in the const pool is present
     *
     * Note that this is an optimisation, and if another transformer is transforming the class anyway then
     * this transformer will always be applied.
     */
    final Set<String> requireConstPoolEntry;

    final boolean cacheable;

    public BytecodeTransformerBuildItem(String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
        this(classToTransform, visitorFunction, null);
    }

    public BytecodeTransformerBuildItem(String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction, Set<String> requireConstPoolEntry) {
        this(false, classToTransform, visitorFunction, requireConstPoolEntry);
    }

    public BytecodeTransformerBuildItem(boolean eager, String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
        this(eager, classToTransform, visitorFunction, null);
    }

    public BytecodeTransformerBuildItem(boolean eager, String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction, boolean cacheable) {
        this(eager, classToTransform, visitorFunction, null, cacheable);
    }

    public BytecodeTransformerBuildItem(boolean eager, String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction, Set<String> requireConstPoolEntry) {
        this(eager, classToTransform, visitorFunction, requireConstPoolEntry, false);
    }

    public BytecodeTransformerBuildItem(boolean eager, String classToTransform,
            BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction, Set<String> requireConstPoolEntry,
            boolean cacheable) {
        this.eager = eager;
        this.classToTransform = classToTransform;
        this.visitorFunction = visitorFunction;
        this.requireConstPoolEntry = requireConstPoolEntry;
        this.cacheable = cacheable;
    }

    public String getClassToTransform() {
        return classToTransform;
    }

    public BiFunction<String, ClassVisitor, ClassVisitor> getVisitorFunction() {
        return visitorFunction;
    }

    public Set<String> getRequireConstPoolEntry() {
        return requireConstPoolEntry;
    }

    public boolean isEager() {
        return eager;
    }

    public boolean isCacheable() {
        return cacheable;
    }
}
