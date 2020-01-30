package io.quarkus.deployment;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * ClassWriter which tries to get around ClassNotFoundExceptions related to reflection usage in
 * getCommonSuperClass.
 *
 * @author Stéphane Épardaud
 */
public class QuarkusClassWriter extends ClassWriter {

    public QuarkusClassWriter(final ClassReader classReader, final int flags) {
        super(classReader, flags);
    }

    public QuarkusClassWriter(final int flags) {
        super(flags);
    }

    @Override
    protected ClassLoader getClassLoader() {
        // the TCCL is safe for transformations when this ClassWriter runs
        return Thread.currentThread().getContextClassLoader();
    }
}
