package io.quarkus.funqy.runtime;

/**
 * A Funqy function found at build time.
 *
 * @param functionClass the class that declares the function method
 * @param methodName the name of the function method
 * @param descriptor the JVM descriptor of the function method
 * @param functionName the name the function is exposed under
 */
public record FunctionDefinition(Class<?> functionClass, String methodName, String descriptor, String functionName) {
}
