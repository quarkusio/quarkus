package io.quarkus.resteasy.runtime;

import static io.quarkus.runtime.TemplateHtmlBuilder.adjustRoot;
import static org.jboss.resteasy.util.HttpHeaderNames.ACCEPT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.request.ServerDrivenNegotiation;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;

import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.http.runtime.devmode.AdditionalRouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

@Provider
@Priority(Priorities.USER + 1)
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    protected static final String META_INF_RESOURCES_SLASH = "META-INF/resources/";
    protected static final String META_INF_RESOURCES = "META-INF/resources";

    private final static Variant JSON_VARIANT = new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, null);
    private final static Variant HTML_VARIANT = new Variant(MediaType.TEXT_HTML_TYPE, (String) null, null);
    private final static List<Variant> VARIANTS = Arrays.asList(JSON_VARIANT, HTML_VARIANT);
    private final static ResourceDescriptionComparator RESOURCE_DESCRIPTION_COMPARATOR = new ResourceDescriptionComparator();
    private final static MethodDescriptionComparator METHOD_DESCRIPTION_COMPARATOR = new MethodDescriptionComparator();

    private volatile static String httpRoot = "";
    private volatile static List<String> servletMappings = Collections.emptyList();
    private volatile static Set<java.nio.file.Path> staticResouceRoots = Collections.emptySet();
    private volatile static List<AdditionalRouteDescription> additionalEndpoints = Collections.emptyList();
    private volatile static Map<String, NonJaxRsClassMappings> nonJaxRsClassNameToMethodPaths = Collections.emptyMap();
    private volatile static List<RouteDescription> reactiveRoutes = Collections.emptyList();

    private static final Logger LOG = Logger.getLogger(NotFoundExceptionMapper.class);

    @Context
    private Registry registry = null;

    @Context
    private HttpHeaders headers;

    public static void setHttpRoot(String rootPath) {
        httpRoot = rootPath;
    }

    public static final class MethodDescription {
        public String method;
        public String fullPath;
        public String produces;
        public String consumes;

        public MethodDescription(String method, String fullPath, String produces, String consumes) {
            super();
            this.method = method;
            this.fullPath = fullPath;
            this.produces = produces;
            this.consumes = consumes;
        }
    }

    public static final class ResourceDescription {
        public final String basePath;
        public final List<MethodDescription> calls;

        public ResourceDescription(String basePath) {
            this.basePath = basePath;
            this.calls = new ArrayList<>();
        }

        public void addMethod(String path, ResourceMethodInvoker method) {
            String produces = mostPreferredOrNull(method.getProduces());
            String consumes = mostPreferredOrNull(method.getConsumes());

            for (String verb : method.getHttpMethods()) {
                calls.add(new MethodDescription(verb, path, produces, consumes));
            }
        }

        private static String mostPreferredOrNull(MediaType[] mediaTypes) {
            if (mediaTypes == null || mediaTypes.length < 1) {
                return null;
            } else {
                return mediaTypes[0].toString();
            }
        }

        public static List<ResourceDescription> fromBoundResourceInvokers(
                Set<Map.Entry<String, List<ResourceInvoker>>> bound) {
            Map<String, ResourceDescription> descriptionMap = new HashMap<>();

            for (Map.Entry<String, List<ResourceInvoker>> entry : bound) {
                for (ResourceInvoker invoker : entry.getValue()) {
                    // skip those for now
                    if (!(invoker instanceof ResourceMethodInvoker)) {
                        continue;
                    }
                    ResourceMethodInvoker method = (ResourceMethodInvoker) invoker;
                    Class<?> resourceClass = method.getResourceClass();
                    String resourceClassName = resourceClass.getName();
                    String basePath = null;
                    NonJaxRsClassMappings nonJaxRsClassMappings = null;
                    Path path = resourceClass.getAnnotation(Path.class);
                    if (path == null) {
                        nonJaxRsClassMappings = nonJaxRsClassNameToMethodPaths.get(resourceClassName);
                        if (nonJaxRsClassMappings != null) {
                            basePath = nonJaxRsClassMappings.getBasePath();
                        }
                    } else {
                        basePath = path.value();
                    }

                    if (basePath == null) {
                        continue;
                    }

                    ResourceDescription description = descriptionMap.get(basePath);
                    if (description == null) {
                        description = new ResourceDescription(basePath);
                        descriptionMap.put(basePath, description);
                    }

                    String subPath = "";
                    for (Annotation annotation : method.getMethodAnnotations()) {
                        if (annotation.annotationType().equals(Path.class)) {
                            subPath = ((Path) annotation).value();
                            break;
                        }
                    }
                    // attempt to find a mapping in the non JAX-RS paths
                    if (subPath.isEmpty() && (nonJaxRsClassMappings != null)) {
                        String methodName = method.getMethod().getName();
                        String subPathFromMethodName = nonJaxRsClassMappings.getMethodNameToPath().get(methodName);
                        if (subPathFromMethodName != null) {
                            subPath = subPathFromMethodName;
                        }
                    }

                    String fullPath = basePath;
                    if (!subPath.isEmpty()) {
                        if (basePath.endsWith("/")) {
                            fullPath += subPath;
                        } else {
                            fullPath = basePath + (subPath.startsWith("/") ? "" : "/") + subPath;
                        }
                    }
                    description.addMethod(fullPath, method);
                }
            }

            List<ResourceDescription> descriptions = new ArrayList<>(descriptionMap.values());
            descriptions.sort(RESOURCE_DESCRIPTION_COMPARATOR);
            for (ResourceDescription description : descriptions) {
                description.calls.sort(METHOD_DESCRIPTION_COMPARATOR);
            }
            return descriptions;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response toResponse(NotFoundException exception) {
        if (registry == null) {
            return respond();
        }

        Map<String, List<ResourceInvoker>> bounded = null;
        if (registry instanceof ResourceMethodRegistry) {
            bounded = ((ResourceMethodRegistry) registry).getBounded();
        } else if (Proxy.isProxyClass(registry.getClass())
                && registry.toString().startsWith(ResourceMethodRegistry.class.getName())) {
            try {
                bounded = (Map<String, List<ResourceInvoker>>) Proxy.getInvocationHandler(registry).invoke(registry,
                        ResourceMethodRegistry.class.getMethod("getBounded"), new Object[0]);
            } catch (Throwable e) {
                //ignore it
            }
        }
        if (bounded == null) {
            return respond();
        }

        List<ResourceDescription> descriptions = ResourceDescription
                .fromBoundResourceInvokers(bounded
                        .entrySet());

        return respond(descriptions);
    }

    private Response respond() {
        Variant variant = selectVariant(headers);

        if (variant == JSON_VARIANT) {
            return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).build();
        }

        if (variant == HTML_VARIANT) {
            TemplateHtmlBuilder sb = new TemplateHtmlBuilder("404 - Resource Not Found", "", "No resources discovered");
            return Response.status(Status.NOT_FOUND).entity(sb.toString()).type(MediaType.TEXT_HTML_TYPE).build();
        }

        return Response.status(Status.NOT_FOUND).build();
    }

    private Response respond(List<ResourceDescription> descriptions) {
        Variant variant = selectVariant(headers);

        if (variant == JSON_VARIANT) {
            return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).build();
        }

        if (variant == HTML_VARIANT) {
            TemplateHtmlBuilder sb = new TemplateHtmlBuilder("404 - Resource Not Found", "", "Resources overview");
            sb.resourcesStart("REST resources");
            for (ResourceDescription resource : descriptions) {
                sb.resourcePath(adjustRoot(httpRoot, resource.basePath));
                for (MethodDescription method : resource.calls) {
                    sb.method(method.method, method.fullPath);
                    if (method.consumes != null) {
                        sb.consumes(method.consumes);
                    }
                    if (method.produces != null) {
                        sb.produces(method.produces);
                    }
                    sb.methodEnd();
                }
                sb.resourceEnd();
            }
            if (descriptions.isEmpty()) {
                sb.noResourcesFound();
            }
            sb.resourcesEnd();

            if (!servletMappings.isEmpty()) {
                sb.resourcesStart("Servlet mappings");
                for (String servletMapping : servletMappings) {
                    sb.servletMapping(adjustRoot(httpRoot, servletMapping));
                }
                sb.resourcesEnd();
            }

            if (!reactiveRoutes.isEmpty()) {
                sb.resourcesStart("Reactive Routes");
                sb.resourceStart();
                for (RouteDescription route : reactiveRoutes) {
                    sb.method(route.getHttpMethod(), route.getPath() != null ? route.getPath() : "/*");
                    sb.listItem(route.getJavaMethod());
                    if (route.getConsumes() != null) {
                        sb.consumes(route.getConsumes());
                    }
                    if (route.getProduces() != null) {
                        sb.produces(route.getProduces());
                    }
                    sb.methodEnd();
                }
                sb.resourceEnd();
                sb.resourcesEnd();
            }

            if (!staticResouceRoots.isEmpty()) {
                List<String> resources = findRealResources();
                if (!resources.isEmpty()) {
                    sb.resourcesStart("Static resources");
                    for (String staticResource : resources) {
                        sb.staticResourcePath(adjustRoot(httpRoot, staticResource));
                    }
                    sb.resourcesEnd();
                }
            }

            if (!additionalEndpoints.isEmpty()) {
                sb.resourcesStart("Additional endpoints");
                for (AdditionalRouteDescription additionalEndpoint : additionalEndpoints) {
                    sb.staticResourcePath(additionalEndpoint.getUri(),
                            additionalEndpoint.getDescription());
                }
                sb.resourcesEnd();
            }

            return Response.status(Status.NOT_FOUND).entity(sb.toString()).type(MediaType.TEXT_HTML_TYPE).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    private List<String> findRealResources() {

        //we need to check for web resources in order to get welcome files to work
        //this kinda sucks
        Set<String> knownFiles = new HashSet<>();
        for (java.nio.file.Path resource : staticResouceRoots) {
            if (resource != null && Files.exists(resource)) {
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
                    knownPaths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isHtmlFileName(String fileName) {
        return fileName.endsWith(".html") || fileName.endsWith(".htm");
    }

    private static Variant selectVariant(HttpHeaders headers) {
        ServerDrivenNegotiation negotiation = new ServerDrivenNegotiation();
        negotiation.setAcceptHeaders(headers.getRequestHeaders().get(ACCEPT));
        return negotiation.getBestMatch(VARIANTS);
    }

    public static void servlets(Map<String, List<String>> servletToMapping) {
        NotFoundExceptionMapper.servletMappings = servletToMapping.values().stream()
                .flatMap(List::stream)
                .sorted()
                .collect(Collectors.toList());
    }

    public static void staticResources(Set<String> knownRoots) {
        NotFoundExceptionMapper.staticResouceRoots = new HashSet<>();
        for (String i : knownRoots) {
            staticResouceRoots.add(Paths.get(i));
        }
    }

    public static void nonJaxRsClassNameToMethodPaths(Map<String, NonJaxRsClassMappings> nonJaxRsPaths) {
        NotFoundExceptionMapper.nonJaxRsClassNameToMethodPaths = nonJaxRsPaths;
    }

    public static void setAdditionalEndpoints(List<AdditionalRouteDescription> additionalEndpoints) {
        NotFoundExceptionMapper.additionalEndpoints = additionalEndpoints;
    }

    public static void setReactiveRoutes(List<RouteDescription> reactiveRoutes) {
        NotFoundExceptionMapper.reactiveRoutes = reactiveRoutes;
    }

    private static class ResourceDescriptionComparator implements Comparator<ResourceDescription> {
        @Override
        public int compare(
                NotFoundExceptionMapper.ResourceDescription d1, NotFoundExceptionMapper.ResourceDescription d2) {
            return d1.basePath.compareTo(d2.basePath);
        }
    }

    private static class MethodDescriptionComparator implements Comparator<MethodDescription> {
        @Override
        public int compare(MethodDescription m1, MethodDescription m2) {
            int fullPathComparison = m1.fullPath.compareTo(m2.fullPath);
            return fullPathComparison == 0 ? m1.method.compareTo(m2.method) : fullPathComparison;
        }
    }
}
