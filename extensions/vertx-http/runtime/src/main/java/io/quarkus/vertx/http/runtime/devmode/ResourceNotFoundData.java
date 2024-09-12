package io.quarkus.vertx.http.runtime.devmode;

import static io.quarkus.runtime.TemplateHtmlBuilder.adjustRoot;

import java.io.IOException;
import java.io.StringWriter;
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

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ResourceNotFoundData {
    private static final Logger LOG = Logger.getLogger(ResourceNotFoundData.class);
    private static volatile List<RouteDescription> runtimeRoutes = null;
    private static volatile List<String> servletMappings = new ArrayList<>();
    private static final String META_INF_RESOURCES = "META-INF/resources";

    private String baseUrl;
    private String httpRoot;
    private List<RouteDescription> endpointRoutes;
    private Set<String> staticRoots;
    private List<AdditionalRouteDescription> additionalEndpoints;

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setHttpRoot(String httpRoot) {
        this.httpRoot = httpRoot;
    }

    public void setEndpointRoutes(List<RouteDescription> endpointRoutes) {
        this.endpointRoutes = endpointRoutes;
    }

    public void setStaticRoots(Set<String> staticRoots) {
        this.staticRoots = staticRoots;
    }

    public void setAdditionalEndpoints(List<AdditionalRouteDescription> additionalEndpoints) {
        this.additionalEndpoints = additionalEndpoints;
    }

    public static void addServlet(String mapping) {
        servletMappings.add(mapping);
    }

    public static void setRuntimeRoutes(List<RouteDescription> routeDescriptions) {
        runtimeRoutes = routeDescriptions;
    }

    public String getHTMLContent() {

        List<RouteDescription> combinedRoutes = getCombinedRoutes();
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(this.baseUrl,
                HEADING, "", "Resources overview", List.of());

        builder.resourcesStart(RESOURCE_ENDPOINTS);

        for (RouteDescription resource : combinedRoutes) {
            builder.resourcePath(adjustRoot(this.httpRoot, resource.getBasePath()));
            for (RouteMethodDescription method : resource.getCalls()) {
                builder.method(method.getHttpMethod(),
                        adjustRoot(this.httpRoot, method.getFullPath()));
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
            builder.resourcesStart(SERVLET_MAPPINGS);
            for (String servletMapping : servletMappings) {
                builder.servletMapping(adjustRoot(this.httpRoot, servletMapping));
            }
            builder.resourcesEnd();
        }

        // Static Resources
        if (!this.staticRoots.isEmpty()) {
            List<String> resources = findRealResources();
            if (!resources.isEmpty()) {
                builder.resourcesStart(STATIC_RESOURCES);
                for (String staticResource : resources) {
                    builder.staticResourcePath(adjustRoot(this.httpRoot, staticResource));
                }
                builder.resourcesEnd();
            }
        }

        // Additional Endpoints
        if (!this.additionalEndpoints.isEmpty()) {
            List<AdditionalRouteDescription> endpoints = getSortedAdditionalRouteDescriptions();
            builder.resourcesStart(ADDITIONAL_ENDPOINTS);
            for (AdditionalRouteDescription additionalEndpoint : endpoints) {
                builder.staticResourcePath(additionalEndpoint.getUri(), additionalEndpoint.getDescription());
            }
            builder.resourcesEnd();
        }

        return builder.toString();
    }

    @NonBlocking
    public JsonObject getJsonContent() {
        List<RouteDescription> combinedRoutes = getCombinedRoutes();
        JsonObject infoMap = new JsonObject();

        // REST Endpoints
        if (!combinedRoutes.isEmpty()) {
            JsonArray r = new JsonArray();
            for (RouteDescription resource : combinedRoutes) {
                String path = adjustRoot(this.httpRoot, resource.getBasePath());

                for (RouteMethodDescription method : resource.getCalls()) {
                    String description = method.getHttpMethod();
                    if (method.getConsumes() != null) {
                        description = description + " (consumes: " + method.getConsumes() + ")";
                    }
                    if (method.getProduces() != null) {
                        description = description + " (produces:" + method.getProduces() + ")";
                    }
                    if (method.getJavaMethod() != null) {
                        description = description + " (java:" + method.getJavaMethod() + ")";
                    }
                    r.add(JsonObject.of(URI, adjustRoot(this.httpRoot, method.getFullPath()),
                            DESCRIPTION, description));
                }
            }
            infoMap.put(RESOURCE_ENDPOINTS, r);
        }

        // Servlets
        if (!servletMappings.isEmpty()) {
            JsonArray sm = new JsonArray();
            for (String servletMapping : servletMappings) {
                sm.add(JsonObject.of(URI, adjustRoot(this.httpRoot, servletMapping), DESCRIPTION,
                        EMPTY));
            }
            infoMap.put(SERVLET_MAPPINGS, sm);
        }

        // Static Resources
        if (!this.staticRoots.isEmpty()) {
            List<String> resources = findRealResources();
            if (!resources.isEmpty()) {
                JsonArray sr = new JsonArray();
                for (String staticResource : resources) {
                    sr.add(JsonObject.of(URI, adjustRoot(this.httpRoot, staticResource), DESCRIPTION,
                            EMPTY));
                }
                infoMap.put(STATIC_RESOURCES, sr);
            }
        }

        // Additional Endpoints
        if (!this.additionalEndpoints.isEmpty()) {
            JsonArray ae = new JsonArray();
            List<AdditionalRouteDescription> endpoints = getSortedAdditionalRouteDescriptions();
            for (AdditionalRouteDescription additionalEndpoint : endpoints) {
                ae.add(JsonObject.of(URI, additionalEndpoint.getUri(), DESCRIPTION, additionalEndpoint.getDescription()));
            }
            infoMap.put(ADDITIONAL_ENDPOINTS, ae);
        }

        return infoMap;

    }

    @NonBlocking
    public String getTextContent() {
        List<RouteDescription> combinedRoutes = getCombinedRoutes();
        try (StringWriter sw = new StringWriter()) {
            sw.write(NL + HEADING + NL);
            sw.write("------------------------" + NL);
            sw.write(NL);
            // REST Endpoints
            if (!combinedRoutes.isEmpty()) {
                sw.write(RESOURCE_ENDPOINTS + NL);
                for (RouteDescription resource : combinedRoutes) {
                    for (RouteMethodDescription method : resource.getCalls()) {
                        String description = method.getHttpMethod();
                        if (method.getConsumes() != null) {
                            description = description + " (consumes: " + method.getConsumes() + ")";
                        }
                        if (method.getProduces() != null) {
                            description = description + " (produces:" + method.getProduces() + ")";
                        }
                        if (method.getJavaMethod() != null) {
                            description = description + " (java:" + method.getJavaMethod() + ")";
                        }
                        sw.write(TAB + "- " + adjustRoot(this.httpRoot, method.getFullPath()) + NL);
                        sw.write(TAB + TAB + "- " + description + NL);
                    }
                }
                sw.write(NL);
            }
            // Servlets
            if (!servletMappings.isEmpty()) {
                sw.write(SERVLET_MAPPINGS + NL);
                for (String servletMapping : servletMappings) {
                    sw.write(TAB + "- " + adjustRoot(this.httpRoot, servletMapping) + NL);
                }
                sw.write(NL);
            }
            // Static Resources
            if (!this.staticRoots.isEmpty()) {
                List<String> resources = findRealResources();
                if (!resources.isEmpty()) {
                    sw.write(STATIC_RESOURCES + NL);
                    for (String staticResource : resources) {
                        sw.write(TAB + "- " + adjustRoot(this.httpRoot, staticResource) + NL);
                    }
                    sw.write(NL);
                }
            }

            // Additional Endpoints
            if (!this.additionalEndpoints.isEmpty()) {
                sw.write(ADDITIONAL_ENDPOINTS + NL);
                List<AdditionalRouteDescription> endpoints = getSortedAdditionalRouteDescriptions();
                for (AdditionalRouteDescription additionalEndpoint : endpoints) {
                    sw.write(TAB + "- " + additionalEndpoint.getUri() + NL);
                    sw.write(TAB + TAB + "- " + additionalEndpoint.getDescription() + NL);
                }
                sw.write(NL);
            }

            return sw.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private List<RouteDescription> getCombinedRoutes() {
        // Endpoints
        List<RouteDescription> combinedRoutes = new ArrayList<>();
        if (this.runtimeRoutes != null) {
            combinedRoutes.addAll(this.runtimeRoutes);
        }
        if (endpointRoutes != null) {
            combinedRoutes.addAll(this.endpointRoutes);
        }
        return combinedRoutes;
    }

    private List<String> findRealResources() {

        //we need to check for web resources in order to get welcome files to work
        //this kinda sucks
        Set<String> knownFiles = new HashSet<>();
        for (String staticResourceRoot : this.staticRoots) {
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
                                    knownFiles.add("/" + rel.toString());
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
                        knownPaths.add("/" + file);
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

    private List<AdditionalRouteDescription> getSortedAdditionalRouteDescriptions() {
        return this.additionalEndpoints.stream().sorted(
                Comparator.comparingInt((AdditionalRouteDescription desc) -> desc.getUri().split("/").length)
                        .thenComparing(AdditionalRouteDescription::getUri))
                .toList();
    }

    private static final String HEADING = "404 - Resource Not Found";
    private static final String RESOURCE_ENDPOINTS = "Resource Endpoints";
    private static final String SERVLET_MAPPINGS = "Servlet mappings";
    private static final String STATIC_RESOURCES = "Static resources";
    private static final String ADDITIONAL_ENDPOINTS = "Additional endpoints";
    private static final String URI = "uri";
    private static final String DESCRIPTION = "description";
    private static final String EMPTY = "";
    private static final String NL = "\n";
    private static final String TAB = "\t";

}
