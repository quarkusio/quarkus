package io.quarkus.resteasy.reactive.server.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.ws.rs.HttpMethod;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;

import io.quarkus.resteasy.reactive.server.runtime.RuntimeResourceMatcher.Match;

/**
 * Without the servlet container, static resources are served for {@code GET} and {@code HEAD} requests before Quarkus
 * REST, so an endpoint matching the path of a static resource is never invoked for these requests, and its
 * annotations, security annotations in particular, do not apply to the static resource. This warns about such
 * endpoints when the application starts.
 */
final class StaticResourceShadowingCheck {

    private static final Logger log = Logger.getLogger(StaticResourceShadowingCheck.class);

    private static final List<String> HTTP_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD);
    // the paths listed per endpoint unless the level of this logger is DEBUG, as an endpoint like the fallback of a
    // single-page application may be shadowed by many static resources
    private static final int MAX_LISTED_PATHS = 5;

    private final RuntimeResourceMatcher matcher;
    // the HTTP root path and the application path, empty or without a trailing slash
    private final String prefix;
    // the HTTP root path without a trailing slash
    private final String rootPath;

    private StaticResourceShadowingCheck(Deployment deployment, String httpRootPath) {
        this.matcher = new RuntimeResourceMatcher(deployment);
        this.prefix = deployment.getPrefix();
        this.rootPath = httpRootPath.endsWith("/") ? httpRootPath.substring(0, httpRootPath.length() - 1) : httpRootPath;
    }

    /**
     * Logs a warning listing the endpoints shadowed by static resources.
     *
     * @param indexPage the index page served for the directories
     * @param staticFilePaths the paths of the static resources, relative to the HTTP root path
     */
    static void check(Deployment deployment, String httpRootPath, String indexPage, Collection<String> staticFilePaths) {
        // lowering the level of this logger also skips the check
        if (!log.isEnabled(Logger.Level.WARN)) {
            return;
        }
        new StaticResourceShadowingCheck(deployment, httpRootPath).run(requestPaths(staticFilePaths, indexPage));
    }

    /**
     * Returns the request paths the static resources are served for, like {@code StaticResourcesRecorder}: the paths
     * of the files, and the paths of the directories containing the index page, ending with a slash.
     */
    private static Set<String> requestPaths(Collection<String> filePaths, String indexPage) {
        String indexFile = indexPage.startsWith("/") ? indexPage : "/" + indexPage;
        Set<String> paths = new TreeSet<>();
        for (String path : filePaths) {
            paths.add(path);
            if (path.endsWith(indexFile)) {
                paths.add(path.substring(0, path.length() - indexFile.length() + 1));
            }
        }
        return paths;
    }

    private void run(Set<String> paths) {
        // endpoint -> HTTP methods -> request paths
        Map<String, Map<String, Set<String>>> shadowed = new LinkedHashMap<>();
        for (String path : paths) {
            String requestPath = rootPath + path;
            // endpoint -> HTTP methods
            Map<String, List<String>> endpoints = new LinkedHashMap<>();
            for (String httpMethod : HTTP_METHODS) {
                Match match = match(requestPath, httpMethod);
                if (match != null) {
                    endpoints.computeIfAbsent(describe(match), k -> new ArrayList<>()).add(httpMethod);
                }
            }
            for (Map.Entry<String, List<String>> endpoint : endpoints.entrySet()) {
                shadowed.computeIfAbsent(endpoint.getKey(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(String.join(", ", endpoint.getValue()), k -> new TreeSet<>())
                        .add(requestPath);
            }
        }
        if (shadowed.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder("""
                Static resources shadow Jakarta REST endpoints: requests with the listed HTTP methods to the listed \
                paths are served the static resource and the endpoint is never invoked, so if the endpoint has \
                security annotations, they do not protect the static resource. Move the static resources or change \
                the endpoint paths, or secure the static resources with configuration, see \
                https://quarkus.io/guides/security-authorize-web-endpoints-reference#authorization-using-configuration:""");
        int maxListedPaths = log.isDebugEnabled() ? Integer.MAX_VALUE : MAX_LISTED_PATHS;
        boolean pathsLeftOut = false;
        for (Map.Entry<String, Map<String, Set<String>>> endpoint : shadowed.entrySet()) {
            for (Map.Entry<String, Set<String>> httpMethods : endpoint.getValue().entrySet()) {
                message.append(System.lineSeparator()).append(httpMethods.getKey()).append(' ');
                pathsLeftOut |= appendPaths(message, httpMethods.getValue(), maxListedPaths);
                message.append(" -> ").append(endpoint.getKey());
            }
        }
        if (pathsLeftOut) {
            message.append(System.lineSeparator()).append("Set quarkus.log.category.\"").append(log.getName())
                    .append("\".level=DEBUG to list all the paths.");
        }
        message.append(System.lineSeparator()).append("If the shadowing is intended, for example when an endpoint is ")
                .append("a fallback for the paths without a static resource, set quarkus.log.category.\"")
                .append(log.getName()).append("\".level=ERROR to disable this warning.");
        log.warn(message);
    }

    /**
     * Appends the paths separated by commas, at most the given number of them, and returns whether paths were left out.
     */
    private static boolean appendPaths(StringBuilder message, Set<String> paths, int maxListedPaths) {
        int listed = 0;
        for (String path : paths) {
            if (listed == maxListedPaths) {
                message.append(" and ").append(paths.size() - listed).append(" more");
                return true;
            }
            if (listed > 0) {
                message.append(", ");
            }
            message.append(path);
            listed++;
        }
        return false;
    }

    /**
     * Returns the resource method a request is dispatched to, or {@code null} if there is none.
     */
    private Match match(String requestPath, String httpMethod) {
        String path;
        if (prefix.isEmpty()) {
            path = requestPath;
        } else if (requestPath.equals(prefix)) {
            path = "/";
        } else if (requestPath.startsWith(prefix + "/")) {
            path = requestPath.substring(prefix.length());
        } else {
            // not dispatched to Quarkus REST
            return null;
        }
        // static resources are matched with the decoded path, Quarkus REST with the encoded one, and characters like
        // '%' or '{' in the name of a static resource are literal
        return matcher.match(Encode.encodePathAsIs(path), httpMethod);
    }

    private static boolean isSubResourceLocator(Match match) {
        return match.resource().getHttpMethod() == null;
    }

    private static String describe(Match match) {
        RuntimeResource resource = match.resource();
        if (resource.getResourceClass() == null) {
            // the resource methods with the same path and different media types are dispatched by a MediaTypeMapper
            return "resource methods with the path template " + match.template();
        }
        String name = resource.getResourceClass().getName() + "#" + resource.getJavaMethodName();
        return isSubResourceLocator(match) ? name + " (sub-resource locator)" : name;
    }
}
