package io.quarkus.bootstrap.app;

/**
 * Applies bytecode transformations to a class during instrumentation-based hot reload.
 */
@FunctionalInterface
public interface ClassTransformer {

    ClassTransformer IDENTITY = (name, bytes) -> bytes;

    byte[] transform(String className, byte[] classData);
}
