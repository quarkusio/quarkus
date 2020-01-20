package io.quarkus.resteasy.runtime;

import static org.jboss.resteasy.util.HttpHeaderNames.ACCEPT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.request.ServerDrivenNegotiation;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;

import io.quarkus.runtime.TemplateHtmlBuilder;

@Provider
@Priority(Priorities.USER + 1)
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private final static Variant JSON_VARIANT = new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, null);
    private final static Variant HTML_VARIANT = new Variant(MediaType.TEXT_HTML_TYPE, (String) null, null);
    private final static List<Variant> VARIANTS = Arrays.asList(JSON_VARIANT, HTML_VARIANT);

    private volatile static String httpRoot = "";
    private volatile static List<String> servletMappings = Collections.EMPTY_LIST;
    private volatile static List<String> staticResources = Collections.EMPTY_LIST;
    private volatile static List<String> additionalEndpoints = Collections.EMPTY_LIST;
    private volatile static Map<String, NonJaxRsClassMappings> nonJaxRsClassNameToMethodPaths = Collections.EMPTY_MAP;

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
            Map<String, ResourceDescription> descriptions = new HashMap<>();

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

                    if (!descriptions.containsKey(basePath)) {
                        descriptions.put(basePath, new ResourceDescription(basePath));
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

                    descriptions.get(basePath).addMethod(basePath + subPath, method);
                }
            }

            return new LinkedList<>(descriptions.values());
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
                sb.resourcePath(adjustRoot(resource.basePath));
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
                    sb.servletMapping(adjustRoot(servletMapping));
                }
                sb.resourcesEnd();
            }

            if (!staticResources.isEmpty()) {
                sb.resourcesStart("Static resources");
                for (String staticResource : staticResources) {
                    sb.staticResourcePath(adjustRoot(staticResource));
                }
                sb.resourcesEnd();
            }

            if (!additionalEndpoints.isEmpty()) {
                sb.resourcesStart("Additional endpoints");
                for (String additionalEndpoint : additionalEndpoints) {
                    sb.staticResourcePath(adjustRoot(additionalEndpoint));
                }
                sb.resourcesEnd();
            }

            return Response.status(Status.NOT_FOUND).entity(sb.toString()).type(MediaType.TEXT_HTML_TYPE).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    private String adjustRoot(String basePath) {
        //httpRoot can optionally end with a slash
        //also some templates want the returned path to start with a / and some don't
        //to make this work we check if the basePath starts with a / or not, and make sure we
        //the return value follows the same pattern

        if (httpRoot.equals("/")) {
            //leave it alone
            return basePath;
        }
        if (basePath.startsWith("/")) {
            if (!httpRoot.endsWith("/")) {
                return httpRoot + basePath;
            }
            return httpRoot.substring(0, httpRoot.length() - 1) + basePath;
        }
        if (httpRoot.endsWith("/")) {
            return httpRoot.substring(1) + basePath;
        }
        return httpRoot.substring(1) + "/" + basePath;
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

    public static void staticResources(Set<String> knownFiles) {
        NotFoundExceptionMapper.staticResources = knownFiles.stream().sorted().collect(Collectors.toList());
    }

    public static void nonJaxRsClassNameToMethodPaths(Map<String, NonJaxRsClassMappings> nonJaxRsPaths) {
        NotFoundExceptionMapper.nonJaxRsClassNameToMethodPaths = nonJaxRsPaths;
    }

    public static void setAdditionalEndpoints(List<String> additionalEndpoints) {
        NotFoundExceptionMapper.additionalEndpoints = additionalEndpoints;
    }
}
