package org.jboss.resteasy.reactive.common.jaxrs;

import java.util.Map;

/**
 * Holds the build-time computed mapping from a resource class name and method name to the raw value of that
 * method's {@link jakarta.ws.rs.Path} annotation, so that {@link UriBuilderImpl#path(Class, String)} can resolve the
 * target method without runtime reflection.
 * <p>
 * The map is populated by a recorder at {@code STATIC_INIT} and only contains unambiguous resolutions: when more than
 * one method with the same simple name is annotated with {@code @Path}, that entry is intentionally omitted so that the
 * reflective fallback in {@link UriBuilderImpl} reproduces the original {@link IllegalArgumentException}. Any lookup miss
 * (unknown class, unmapped method, or an environment where the map was never populated, e.g. a REST client only
 * application) likewise falls back to reflection.
 */
public final class ResourceMethodPathRegistry {

    private static volatile Map<String, Map<String, String>> resourceMethodPaths = Map.of();

    private ResourceMethodPathRegistry() {
    }

    public static void setResourceMethodPaths(Map<String, Map<String, String>> paths) {
        resourceMethodPaths = paths;
    }

    /**
     * Returns the raw {@code @Path} value for the given resource class and method name, or {@code null} if there is no
     * build-time entry, in which case the caller must fall back to reflection.
     */
    public static String getPath(String className, String methodName) {
        Map<String, String> methods = resourceMethodPaths.get(className);
        return methods == null ? null : methods.get(methodName);
    }

    public static void clear() {
        resourceMethodPaths = Map.of();
    }
}
