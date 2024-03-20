package io.quarkus.vertx.http.runtime.devmode;

import static io.quarkus.runtime.TemplateHtmlBuilder.adjustRoot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.util.ClassPathUtils;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Lists all routes when no route matches the path in the dev mode.
 */
public class ResourceNotFoundHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(ResourceNotFoundHandler.class);
    protected static final String META_INF_RESOURCES = "META-INF/resources";

    public static volatile List<RouteDescription> runtimeRoutes;
    private static volatile List<String> servletMappings = new ArrayList<>();

    private final String baseUrl;
    private final String httpRoot;
    private final List<RouteDescription> routes;
    private final Set<String> staticResourceRoots;
    private final List<AdditionalRouteDescription> additionalEndpoints;

    public ResourceNotFoundHandler(String baseUrl,
            String httpRoot,
            List<RouteDescription> routes,
            Set<String> staticResourceRoots,
            List<AdditionalRouteDescription> additionalEndpoints) {
        this.baseUrl = baseUrl;
        this.httpRoot = httpRoot;
        this.routes = routes;
        this.staticResourceRoots = staticResourceRoots;
        this.additionalEndpoints = additionalEndpoints;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        String header = routingContext.request().getHeader("Accept");
        if (header != null && header.startsWith("application/json")) {
            handleJson(routingContext);
        } else {
            handleHTML(routingContext);
        }
    }

    private void handleJson(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end();
    }

    private void handleHTML(RoutingContext routingContext) {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(baseUrl, "404 - Resource Not Found", "", "Resources overview");

        // Endpoints
        List<RouteDescription> combinedRoutes = new ArrayList<>();
        if (runtimeRoutes != null) {
            combinedRoutes.addAll(runtimeRoutes);
        }
        if (routes != null) {
            combinedRoutes.addAll(routes);
        }

        builder.resourcesStart("Resource Endpoints");

        for (RouteDescription resource : combinedRoutes) {
            builder.resourcePath(adjustRoot(httpRoot, resource.getBasePath()));
            for (RouteMethodDescription method : resource.getCalls()) {
                builder.method(method.getHttpMethod(), method.getFullPath());
                if (method.getJavaMethod() != null) {
                    builder.listItem(method.getJavaMethod());
                }
                if (method.getConsumes() != null) {
                    builder.consumes(method.getConsumes());
                }
                if (method.getProduces() != null) {
                    builder.produces(method.getProduces());
                }
                builder.methodEnd();
            }
            builder.resourceEnd();
        }
        if (combinedRoutes.isEmpty()) {
            builder.noResourcesFound();
        }
        builder.resourcesEnd();

        if (!servletMappings.isEmpty()) {
            builder.resourcesStart("Servlet mappings");
            for (String servletMapping : servletMappings) {
                builder.servletMapping(adjustRoot(httpRoot, servletMapping));
            }
            builder.resourcesEnd();
        }

        // Static Resources
        if (!staticResourceRoots.isEmpty()) {
            List<String> resources = findRealResources();
            if (!resources.isEmpty()) {
                builder.resourcesStart("Static resources");
                for (String staticResource : resources) {
                    builder.staticResourcePath(adjustRoot(httpRoot, staticResource));
                }
                builder.resourcesEnd();
            }
        }

        // Additional Endpoints
        if (!additionalEndpoints.isEmpty()) {
            builder.resourcesStart("Additional endpoints");
            for (AdditionalRouteDescription additionalEndpoint : additionalEndpoints) {
                builder.staticResourcePath(additionalEndpoint.getUri(), additionalEndpoint.getDescription());
            }
            builder.resourcesEnd();
        }

        routingContext.response()
                .setStatusCode(404)
                .putHeader("content-type", "text/html; charset=utf-8")
                .end(builder.toString());
    }

    private List<String> findRealResources() {

        //we need to check for web resources in order to get welcome files to work
        //this kinda sucks
        Set<String> knownFiles = new HashSet<>();
        for (String staticResourceRoot : staticResourceRoots) {
            if (staticResourceRoot != null) {
                Path resource = Paths.get(staticResourceRoot);
                if (Files.exists(resource)) {
                    try (Stream<java.nio.file.Path> fileTreeElements = Files.walk(resource)) {
                        fileTreeElements.forEach(new Consumer<java.nio.file.Path>() {
                            @Override
                            public void accept(java.nio.file.Path path) {
                                // Skip META-INF/resources entry
                                if (resource.equals(path)) {
                                    return;
                                }
                                java.nio.file.Path rel = resource.relativize(path);
                                if (!Files.isDirectory(path)) {
                                    knownFiles.add(rel.toString());
                                }
                            }
                        });
                    } catch (IOException e) {
                        LOG.error("Failed to read static resources", e);
                    }
                }
            }
        }
        try {
            ClassPathUtils.consumeAsPaths(META_INF_RESOURCES, p -> {
                collectKnownPaths(p, knownFiles);
            });
        } catch (IOException e) {
            LOG.error("Failed to read static resources", e);
        }

        //limit to 1000 to not have to many files to display
        return knownFiles.stream().filter(this::isHtmlFileName).limit(1000).distinct().sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private void collectKnownPaths(java.nio.file.Path resource, Set<String> knownPaths) {
        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<java.nio.file.Path>() {
                @Override
                public FileVisitResult visitFile(java.nio.file.Path p, BasicFileAttributes attrs)
                        throws IOException {
                    String file = resource.relativize(p).toString();
                    // Windows has a backslash
                    file = file.replace('\\', '/');
                    if (!file.startsWith("_static/") && !file.startsWith("webjars/")) {
                        knownPaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isHtmlFileName(String fileName) {
        return fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".xhtml");
    }

    public static void addServlet(String mapping) {
        servletMappings.add(mapping);
    }
}
