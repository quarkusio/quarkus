package io.quarkus.restclient.runtime;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;

/**
 * feature that set's the URLConfig
 */
@ConstrainedTo(RuntimeType.CLIENT)
public class PathFeatureHandler implements DynamicFeature {
    private static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        context.property("UrlPathTemplate", constructPath(resourceInfo.getResourceMethod()));
    }

    private String constructPath(Method methodInfo) {
        Path annotation = methodInfo.getAnnotation(Path.class);

        StringBuilder stringBuilder;
        if (annotation != null) {
            stringBuilder = new StringBuilder(slashify(annotation.value()));
        } else {
            stringBuilder = new StringBuilder();
        }

        // Look for @Path annotation on the class
        annotation = methodInfo.getDeclaringClass().getAnnotation(Path.class);
        if (annotation != null) {
            stringBuilder.insert(0, slashify(annotation.value()));
        }

        // Now make sure there is a leading path, and no duplicates
        return MULTIPLE_SLASH_PATTERN.matcher('/' + stringBuilder.toString()).replaceAll("/");
    }

    String slashify(String path) {
        // avoid doubles later. Empty for now
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        // remove doubles
        path = MULTIPLE_SLASH_PATTERN.matcher(path).replaceAll("/");
        // Label value consistency: result should not end with a slash
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty() || path.startsWith("/")) {
            return path;
        }
        return '/' + path;
    }
}
