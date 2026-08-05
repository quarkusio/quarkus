package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Loads {@link Target} via the protected {@code ClassLoader.findClass(String)}
 * in its constructor. Tests that this seed is recognized by the class-loading chain analysis.
 */
public class FindClassLoaderUtil extends ClassLoader {
    public FindClassLoaderUtil() throws Exception {
        super.findClass("io.quarkus.deployment.pkg.steps.treeshake.Target");
    }
}
