package io.quarkus.deployment.pkg.steps.treeshake;

/**
 * Utility that loads a class by name via {@code Class.forName()}.
 * Used with {@link ForNameProvider} to test cross-class call chain analysis.
 */
public class ForNameUtil {
    public static Class<?> load(String name) throws Exception {
        return Class.forName(name);
    }
}
