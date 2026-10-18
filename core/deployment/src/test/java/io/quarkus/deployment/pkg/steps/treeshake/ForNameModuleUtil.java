package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Loads {@link Target} via {@code Class.forName(Module, String)} in its constructor.
 * Tests that the module-based forName seed is recognized by the class-loading chain analysis.
 */
public class ForNameModuleUtil {
    public ForNameModuleUtil() {
        Class.forName(ForNameModuleUtil.class.getModule(),
                "io.quarkus.deployment.pkg.steps.treeshake.Target");
    }
}
