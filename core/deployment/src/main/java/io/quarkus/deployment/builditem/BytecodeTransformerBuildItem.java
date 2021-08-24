package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.builder.item.MultiBuildItem;

public final class BytecodeTransformerBuildItem extends MultiBuildItem {

    /**
     * If this is true it means the class should be loaded eagerly by a thread pool in dev mode
     * on multi threaded systems.
     * <p>
     * Transformation is expensive, so doing it this way can speed up boot time.
     */
    final boolean eager;
    final String classToTransform;
    final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;

    /**
     * Function that can be applied to the inout bytes before it is passed into ASM. This should only be used
     * in very specific circumstances. At the moment the only known valid use case is jacoco, which needs
     * access to the unmodified class file bytes.
     */
    final BiFunction<String, byte[], byte[]> inputTransformer;

    /**
     * A set of class names that need to be present in the const pool for the transformation to happen. These
     * need to be in JVM internal format.
     * <p>
     * The transformation is only applied if at least one of the entries in the const pool is present
     * <p>
     * Note that this is an optimisation, and if another transformer is transforming the class anyway then
     * this transformer will always be applied.
     */
    final Set<String> requireConstPoolEntry;

    final boolean cacheable;

    final int classReaderOptions;

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
        Objects.requireNonNull(visitorFunction, "visitorFunction");
        this.eager = eager;
        this.classToTransform = classToTransform;
        this.visitorFunction = visitorFunction;
        this.requireConstPoolEntry = requireConstPoolEntry;
        this.cacheable = cacheable;
        this.inputTransformer = null;
        this.classReaderOptions = 0;
    }

    public BytecodeTransformerBuildItem(Builder builder) {
        this.eager = builder.eager;
        this.classToTransform = builder.classToTransform;
        this.visitorFunction = builder.visitorFunction;
        this.requireConstPoolEntry = builder.requireConstPoolEntry;
        this.cacheable = builder.cacheable;
        this.inputTransformer = builder.inputTransformer;
        this.classReaderOptions = builder.classReaderOptions;
        if (visitorFunction == null && inputTransformer == null) {
            throw new IllegalArgumentException("One of either visitorFunction or inputTransformer must be set");
        }
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

    public int getClassReaderOptions() {
        return classReaderOptions;
    }

    public BiFunction<String, byte[], byte[]> getInputTransformer() {
        return inputTransformer;
    }

    public static class Builder {
        public BiFunction<String, byte[], byte[]> inputTransformer;
        private String classToTransform;
        private BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;
        private Set<String> requireConstPoolEntry = null;
        private boolean eager = false;
        private boolean cacheable = false;
        private int classReaderOptions = 0;

        public Builder setInputTransformer(BiFunction<String, byte[], byte[]> inputTransformer) {
            this.inputTransformer = inputTransformer;
            return this;
        }

        public Builder setClassToTransform(String classToTransform) {
            this.classToTransform = classToTransform;
            return this;
        }

        public Builder setVisitorFunction(BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
            this.visitorFunction = visitorFunction;
            return this;
        }

        public Builder setRequireConstPoolEntry(Set<String> requireConstPoolEntry) {
            this.requireConstPoolEntry = requireConstPoolEntry;
            return this;
        }

        public Builder setEager(boolean eager) {
            this.eager = eager;
            return this;
        }

        public Builder setCacheable(boolean cacheable) {
            this.cacheable = cacheable;
            return this;
        }

        public Builder setClassReaderOptions(int classReaderOptions) {
            this.classReaderOptions = classReaderOptions;
            return this;
        }

        public BytecodeTransformerBuildItem build() {
            return new BytecodeTransformerBuildItem(this);
        }
    }

}
