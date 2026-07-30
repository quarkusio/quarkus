package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

final class JaxRsPathCollector {

    static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");
    static final DotName APPLICATION_PATH = DotName.createSimple("jakarta.ws.rs.ApplicationPath");

    private JaxRsPathCollector() {
    }

    /**
     * Collects all unique endpoint paths from the Jandex index, resolving them against the
     * HTTP root path and any {@code @ApplicationPath}.
     *
     * @param index the combined Jandex index
     * @param httpRootPath the value of {@code quarkus.http.root-path} (typically {@code /})
     * @return deduplicated list of absolute endpoint paths
     */
    static List<String> collect(IndexView index, String httpRootPath) {
        String applicationPath = resolveApplicationPath(index);
        String basePath = appendPath(sanitize(httpRootPath), sanitize(applicationPath));

        Set<String> paths = new LinkedHashSet<>();
        Collection<AnnotationInstance> pathAnnotations = index.getAnnotations(PATH);

        for (AnnotationInstance annotation : pathAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            MethodInfo method = annotation.target().asMethod();
            ClassInfo declaringClass = method.declaringClass();

            String classPath = classPath(declaringClass);
            if (classPath == null) {
                continue;
            }
            String methodPath = annotation.value().asString();
            String fullPath = appendPath(basePath, appendPath(sanitize(classPath), sanitize(methodPath)));
            if (!fullPath.isBlank()) {
                paths.add(fullPath);
            }
        }

        for (AnnotationInstance annotation : pathAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo clazz = annotation.target().asClass();
            boolean hasMethodPaths = pathAnnotations.stream()
                    .anyMatch(a -> a.target().kind() == AnnotationTarget.Kind.METHOD
                            && a.target().asMethod().declaringClass().name().equals(clazz.name()));
            if (!hasMethodPaths) {
                String classPath = annotation.value().asString();
                String fullPath = appendPath(basePath, sanitize(classPath));
                if (!fullPath.isBlank()) {
                    paths.add(fullPath);
                }
            }
        }

        return new ArrayList<>(paths);
    }

    private static String resolveApplicationPath(IndexView index) {
        Collection<AnnotationInstance> appPaths = index.getAnnotations(APPLICATION_PATH);
        if (appPaths.isEmpty()) {
            return null;
        }
        AnnotationInstance first = appPaths.iterator().next();
        return first.value() != null ? first.value().asString() : null;
    }

    private static String classPath(ClassInfo clazz) {
        AnnotationInstance classPathAnnotation = clazz.declaredAnnotation(PATH);
        if (classPathAnnotation == null) {
            return null;
        }
        return classPathAnnotation.value() != null ? classPathAnnotation.value().asString() : null;
    }

    static String sanitize(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        path = path.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    static String appendPath(String prefix, String suffix) {
        if (prefix == null || prefix.isBlank()) {
            return suffix == null ? "" : suffix;
        }
        if (suffix == null || suffix.isBlank()) {
            return prefix;
        }
        if (prefix.endsWith("/") && suffix.startsWith("/")) {
            return prefix + suffix.substring(1);
        }
        if (!prefix.endsWith("/") && !suffix.startsWith("/")) {
            return prefix + "/" + suffix;
        }
        return prefix + suffix;
    }

    /**
     * Strips the path template portion from a path, returning the static prefix.
     * For example, {@code /users/{id}/posts} becomes {@code /users}.
     */
    static String staticPrefix(String path) {
        int idx = path.indexOf('{');
        if (idx < 0) {
            return path;
        }
        String prefix = path.substring(0, idx);
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix.isBlank() ? "/" : prefix;
    }

    static boolean hasPathTemplate(String path) {
        return path.contains("{");
    }
}
