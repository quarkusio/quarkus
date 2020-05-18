package io.quarkus.test.junit.internal;

/**
 * Strategy to deep clone an object
 *
 * Used in order to clone an object loaded from one ClassLoader into another
 */
public interface DeepClone {

    Object clone(Object objectToClone);
}
