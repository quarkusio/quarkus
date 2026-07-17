package io.quarkus.modular.spi;

import java.util.Set;

/**
 * Shared utility methods for inspecting classpath resource paths.
 */
public final class ClassPathUtils {

    private static final String DOT_CLASS = ".class";
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String VERSIONS_PREFIX = "META-INF/versions/";

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

    /**
     * Strip the multi-release {@code META-INF/versions/<N>/} prefix from a resource path.
     * For example, {@code "META-INF/versions/11/com/example/Foo.class"} becomes {@code "com/example/Foo.class"}.
     *
     * @param resourcePath the resource path within a JAR
     * @return the path relative to the versioned root, or the original path if it is not versioned
     */
    public static String stripVersionedPrefix(String resourcePath) {
        if (resourcePath.startsWith(VERSIONS_PREFIX)) {
            int idx = resourcePath.indexOf('/', VERSIONS_PREFIX.length());
            if (idx != -1) {
                return resourcePath.substring(idx + 1);
            }
        }
        return resourcePath;
    }

    /**
     * Check whether a resource path is a class file entry (including multi-release versioned entries)
     * whose class is not in the reachable set, and should therefore be removed by tree-shaking.
     * Non-class resources are never considered unreachable.
     *
     * @param resourcePath the resource path within a JAR
     * @param reachableClassNames dot-separated names of reachable classes
     * @return {@code true} if the entry is an unreachable class
     */
    public static boolean isUnreachableClassEntry(String resourcePath, Set<String> reachableClassNames) {
        String path = stripVersionedPrefix(resourcePath);
        return isClassEntry(path) && !reachableClassNames.contains(resourcePathToClassName(path));
    }

    /**
     * Get the dot-separated package name of a resource path (after stripping any multi-release prefix).
     *
     * @param resourcePath the resource path within a JAR
     * @return the package name, or the empty string for entries in the root directory
     */
    public static String packageOf(String resourcePath) {
        String path = stripVersionedPrefix(resourcePath);
        int idx = path.lastIndexOf('/');
        return idx == -1 ? "" : path.substring(0, idx).replace('/', '.');
    }
}
