package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Loads {@link Target} via {@code Class.forName(String, boolean, ClassLoader)} in its constructor.
 * Tests that the 3-argument forName seed is recognized by the class-loading chain analysis.
 */
public class ForName3ArgUtil {
    public ForName3ArgUtil() throws Exception {
        Class.forName("io.quarkus.deployment.pkg.steps.treeshake.Target",
                true, Thread.currentThread().getContextClassLoader());
    }
}
