package io.quarkus.modular.spi;

/**
 * Shared utility methods for inspecting classpath resource paths.
 */
public final class ClassPathUtils {

    private static final String DOT_CLASS = ".class";
    private static final String MODULE_INFO_CLASS = "module-info.class";

    private ClassPathUtils() {
    }

    /**
     * Check whether a resource path represents a regular {@code .class} file entry,
     * excluding {@code module-info.class} descriptors.
     *
     * @param resourcePath the resource path within a JAR
     * @return {@code true} if this is a regular class file entry
     */
    public static boolean isClassEntry(String resourcePath) {
        return resourcePath.endsWith(DOT_CLASS) && !isModuleInfoEntry(resourcePath);
    }

    /**
     * Check whether a resource path is a {@code module-info.class} descriptor.
     *
     * @param resourcePath the resource path within a JAR
     * @return {@code true} if the path is a {@code module-info.class}
     */
    public static boolean isModuleInfoEntry(String resourcePath) {
        return resourcePath.endsWith(MODULE_INFO_CLASS) &&
                (resourcePath.length() == MODULE_INFO_CLASS.length() ||
                        resourcePath.charAt(resourcePath.length() - MODULE_INFO_CLASS.length() - 1) == '/');
    }

    /**
     * Convert a JAR resource path for a class file to a dot-separated class name.
     * For example, {@code "com/example/Foo.class"} becomes {@code "com.example.Foo"}.
     *
     * @param resourcePath the resource path (must end with {@code .class})
     * @return the dot-separated class name
     */
    public static String resourcePathToClassName(String resourcePath) {
        return resourcePath.substring(0, resourcePath.length() - DOT_CLASS.length()).replace('/', '.');
    }
}
