package io.quarkus.resteasy.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResourceLocatorInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.util.Encode;

/**
 * Static resources are served for {@code GET} and {@code HEAD} requests before RESTEasy, so an endpoint matching the
 * path of a static resource is never invoked for these requests, and its annotations, security annotations in
 * particular, do not apply to the static resource. This warns about such endpoints when the application starts.
 * <p>
 * The request paths of the static resources are matched by the registry of the RESTEasy deployment, the same way
 * requests are, without invoking the endpoints.
 */
public final class StaticResourceShadowingCheck {

    private static final Logger log = Logger.getLogger(StaticResourceShadowingCheck.class);

    private static final List<String> HTTP_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD);
    // the paths listed per endpoint unless the level of this logger is DEBUG, as an endpoint like the fallback of a
    // single-page application may be shadowed by many static resources
    private static final int MAX_LISTED_PATHS = 5;

    private final Registry registry;
    // the path requests are dispatched to RESTEasy under, see ServletUtil#extractUriInfo and VertxUtil#extractUriInfo
    private final String contextPath;
    private final String pathPrefix;

    public StaticResourceShadowingCheck(Registry registry, String contextPath) {
        this.registry = registry;
        this.contextPath = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        this.pathPrefix = this.contextPath.endsWith("/")
                ? this.contextPath.substring(0, this.contextPath.length() - 1)
                : this.contextPath;
    }

    /**
     * Returns the request paths the static resources are served for: the paths of the files, and the paths of the
     * directories containing an index page, ending with a slash.
     */
    private static Set<String> requestPaths(Collection<String> filePaths, Collection<String> indexPages) {
        List<String> indexFiles = new ArrayList<>(indexPages.size());
        for (String indexPage : indexPages) {
            indexFiles.add(indexPage.startsWith("/") ? indexPage : "/" + indexPage);
        }
        Set<String> paths = new TreeSet<>();
        for (String filePath : filePaths) {
            // the servlet container has the paths of the static files without the leading slash
            String path = filePath.startsWith("/") ? filePath : "/" + filePath;
            paths.add(path);
            for (String indexFile : indexFiles) {
                if (path.endsWith(indexFile)) {
                    paths.add(path.substring(0, path.length() - indexFile.length() + 1));
                }
            }
        }
        return paths;
    }

    /**
     * Logs a warning listing the endpoints shadowed by the static resources.
     *
     * @param pathPrefix the path the static resources are served under: the HTTP root path, or the servlet context
     *        path
     * @param filePaths the paths of the static files, relative to the prefix, with or without the leading slash
     * @param indexPages the files served for the directories containing them: the index page, or the welcome files
     */
    public void check(String pathPrefix, Collection<String> filePaths, Collection<String> indexPages) {
        // lowering the level of this logger also skips the check
        if (!log.isEnabled(Logger.Level.WARN)) {
            return;
        }
        String prefix = pathPrefix.endsWith("/") ? pathPrefix.substring(0, pathPrefix.length() - 1) : pathPrefix;
        // endpoint -> HTTP methods -> request paths
        Map<String, Map<String, Set<String>>> shadowed = new LinkedHashMap<>();
        for (String path : requestPaths(filePaths, indexPages)) {
            String requestPath = prefix + path;
            // endpoint -> HTTP methods
            Map<String, List<String>> endpoints = new LinkedHashMap<>();
            for (String httpMethod : HTTP_METHODS) {
                ResourceInvoker endpoint = match(requestPath, httpMethod);
                if (endpoint != null) {
                    endpoints.computeIfAbsent(describe(endpoint), k -> new ArrayList<>()).add(httpMethod);
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
     * Returns the endpoint RESTEasy dispatches a request to, or {@code null} if there is none.
     */
    private ResourceInvoker match(String requestPath, String httpMethod) {
        if (!requestPath.equals(pathPrefix) && !requestPath.startsWith(pathPrefix + "/")) {
            // not dispatched to RESTEasy
            return null;
        }
        try {
            // static resources are matched with the decoded path, RESTEasy with the encoded one, and characters like
            // '%' or '{' in the name of a static resource are literal
            MockHttpRequest request = MockHttpRequest.create(httpMethod,
                    "http://localhost" + Encode.encodePathAsIs(requestPath), null, contextPath);
            return registry.getResourceInvoker(request);
        } catch (WebApplicationException e) {
            // e.g. NotFoundException
            return null;
        } catch (RuntimeException e) {
            // e.g. several endpoints match and RESTEasy is configured to fail fast, which must not fail the startup
            log.debugf(e, "Failed to match the %s %s request with the Jakarta REST endpoints", httpMethod, requestPath);
            return null;
        }
    }

    private static String describe(ResourceInvoker endpoint) {
        Class<?> resourceClass = endpoint instanceof ResourceMethodInvoker resourceMethod
                ? resourceMethod.getResourceClass()
                : endpoint.getMethod().getDeclaringClass();
        String name = resourceClass.getName() + "#" + endpoint.getMethod().getName();
        return endpoint instanceof ResourceLocatorInvoker ? name + " (sub-resource locator)" : name;
    }
}
