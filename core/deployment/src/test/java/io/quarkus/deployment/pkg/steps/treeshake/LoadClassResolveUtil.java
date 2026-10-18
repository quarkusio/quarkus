package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Loads {@link Target} via the protected {@code ClassLoader.loadClass(String, boolean)}
 * in its constructor. Tests that this seed is recognized by the class-loading chain analysis.
 */
public class LoadClassResolveUtil extends ClassLoader {
    public LoadClassResolveUtil() throws Exception {
        super.loadClass("io.quarkus.deployment.pkg.steps.treeshake.Target", false);
    }
}
