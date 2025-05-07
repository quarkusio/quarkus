package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Transform a class using ASM {@link ClassVisitor}. Note that the transformation is performed after assembling the
 * index and thus the changes won't be visible to any processor steps relying on the index.
 * <p>
 * You may consider using {@code io.quarkus.arc.deployment.AnnotationsTransformerBuildItem} if your transformation
 * should be visible for Arc. See also
 * <a href="https://quarkus.io/version/main/guides/cdi-integration#annotations_transformer_build_item">I Need To
 * Transform Annotation Metadata</a> section of Quarkus CDI integration guide.
 */
public final class BytecodeTransformerBuildItem extends MultiBuildItem {

    final String classToTransform;
    final BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;

    /**
     * Function that can be applied to the input bytes before it is passed into ASM. This should only be used
     * in very specific circumstances. At the moment the only known valid use case is JaCoCo, which needs
     * access to the unmodified class file bytes.
     */
    final BiFunction<String, byte[], byte[]> inputTransformer;

    /**
     * A set of class names that need to be present in the const pool for the transformation to happen. These
     * need to be in JVM internal format.
     * <p>
     * The transformation is only applied if at least one of the entries in the const pool is present.
     * <p>
     * Note that this is an optimisation, and if another transformer is transforming the class anyway then
     * this transformer will always be applied.
     */
    final Set<String> requireConstPoolEntry;

    final boolean cacheable;

    final int classReaderOptions;

    final boolean continueOnFailure;

    final int priority;

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
        this.classToTransform = classToTransform;
        this.visitorFunction = visitorFunction;
        this.requireConstPoolEntry = requireConstPoolEntry;
        this.cacheable = cacheable;
        this.inputTransformer = null;
        this.classReaderOptions = 0;
        this.continueOnFailure = false;
        this.priority = 0;
    }

    public BytecodeTransformerBuildItem(Builder builder) {
        this.classToTransform = builder.classToTransform;
        this.visitorFunction = builder.visitorFunction;
        this.requireConstPoolEntry = builder.requireConstPoolEntry;
        this.cacheable = builder.cacheable;
        this.inputTransformer = builder.inputTransformer;
        this.classReaderOptions = builder.classReaderOptions;
        this.continueOnFailure = builder.continueOnFailure;
        this.priority = builder.priority;
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

    @Deprecated(since = "3.11", forRemoval = true)
    public boolean isEager() {
        return false;
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

    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    /**
     * Bytecode transformers are applied in ascending priority order. That is, lower priority
     * value means the transformer is applied sooner, and higher priority value means
     * the transformer is applied later.
     * <p>
     * This applies directly to {@link #inputTransformer} functions: an input transformer
     * function with lower priority is applied first and its result is passed to the transformer
     * function with higher priority.
     * <p>
     * It is a bit counter-intuitive when it comes to the {@link #visitorFunction}. The visitor
     * function doesn't directly transform bytecode; instead, it constructs an ASM
     * {@link ClassVisitor} from an earlier class visitor. This means that the priority order
     * is effectively turned around: the later a bytecode transformer is called,
     * the <em>higher</em> in the class visitor chain it ends up, and the <em>sooner</em>
     * is the visitor eventually called.
     * <p>
     * The priority value defaults to {@code 0}.
     */
    public int getPriority() {
        return priority;
    }

    public static class Builder {
        public BiFunction<String, byte[], byte[]> inputTransformer;
        public boolean continueOnFailure;
        private String classToTransform;
        private BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction;
        private Set<String> requireConstPoolEntry = null;
        private boolean cacheable = false;
        private int classReaderOptions = 0;
        private int priority = 0;

        public Builder setContinueOnFailure(boolean continueOnFailure) {
            this.continueOnFailure = continueOnFailure;
            return this;
        }

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

        @Deprecated(since = "3.11", forRemoval = true)
        public Builder setEager(boolean eager) {
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

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public BytecodeTransformerBuildItem build() {
            return new BytecodeTransformerBuildItem(this);
        }
    }

}
