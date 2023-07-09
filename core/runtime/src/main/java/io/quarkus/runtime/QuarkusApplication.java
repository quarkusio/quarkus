package io.quarkus.runtime;

/**
 * This is usually used for command mode applications with a startup logic. The logic is executed inside
 * {@link QuarkusApplication#run} method before the main application exits.
 */
public interface QuarkusApplication {

    int run(String... args) throws Exception;
}
