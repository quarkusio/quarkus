package io.quarkus.deployment.pkg.steps.treeshake;

import java.lang.invoke.MethodHandles;

/**
 * Loads {@link Target} via {@code MethodHandles.Lookup.findClass(String)} in its constructor.
 * Tests that this seed is recognized by the class-loading chain analysis.
 */
public class MHFindClassUtil {
    public MHFindClassUtil() throws Exception {
        MethodHandles.lookup().findClass("io.quarkus.deployment.pkg.steps.treeshake.Target");
    }
}
