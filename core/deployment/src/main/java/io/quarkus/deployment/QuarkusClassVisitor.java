package io.quarkus.deployment;

import org.objectweb.asm.ClassVisitor;

/**
 * A subclass of {@link ClassVisitor} that allows carrying around data
 * that are useful in the context of Quarkus bytecode transformations.
 * Class visitors that require access to these data should extend this class.
 */
public abstract class QuarkusClassVisitor extends ClassVisitor {
    public QuarkusClassVisitor(int api) {
        super(api);
    }

    public QuarkusClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    // ---

    // this could possibly be a Map<String, Object> or something like that,
    // but there's no need at the moment
    private int originalClassReaderOptions;

    public int getOriginalClassReaderOptions() {
        return originalClassReaderOptions;
    }

    public void setOriginalClassReaderOptions(int originalClassReaderOptions) {
        this.originalClassReaderOptions = originalClassReaderOptions;
    }
}
