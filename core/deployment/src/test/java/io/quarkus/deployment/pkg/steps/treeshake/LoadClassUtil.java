package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Loads {@link Target} via {@code ClassLoader.loadClass(String)} in its constructor.
 * Tests that this seed method is recognized by the class-loading chain analysis.
 */
public class LoadClassUtil {
    public LoadClassUtil() throws Exception {
        Thread.currentThread().getContextClassLoader()
                .loadClass("io.quarkus.deployment.pkg.steps.treeshake.Target");
    }
}
