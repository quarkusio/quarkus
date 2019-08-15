/**
 * 
 */
package io.quarkus.resteasy.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;

import io.quarkus.runtime.TemplateHtmlBuilder;

/**
 * 
 */
@Provider
@Priority(Priorities.USER + 1)
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Context
    private Registry registry = null;

    @Context
    private HttpHeaders headers;

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
        public String basePath;
        public List<MethodDescription> calls;

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
                Class<?> resourceClass = ((ResourceMethodInvoker) entry.getValue().get(0)).getResourceClass();
                Path path = resourceClass.getAnnotation(Path.class);
                if (path == null) {
                    continue;
                }
                String basePath = path.value();

                if (!descriptions.containsKey(basePath)) {
                    descriptions.put(basePath, new ResourceDescription(basePath));
                }

                for (ResourceInvoker invoker : entry.getValue()) {
                    ResourceMethodInvoker method = (ResourceMethodInvoker) invoker;

                    String subPath = "";
                    for (Annotation annotation : method.getMethodAnnotations()) {
                        if (annotation.annotationType().equals(Path.class)) {
                            subPath = ((Path) annotation).value();
                            break;
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
        if (headers.getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.errorMessage = "Resource Not Found";
            return Response.status(Status.NOT_FOUND).entity(errorMessage).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        TemplateHtmlBuilder sb = new TemplateHtmlBuilder();
        return Response.status(Status.NOT_FOUND).entity(sb.toString()).build();
    }

    private Response respond(List<ResourceDescription> descriptions) {
        if (headers.getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)) {
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.errorMessage = "Resource Not Found";
            errorMessage.existingResourcesDetails = descriptions;
            return Response.status(Status.NOT_FOUND).entity(errorMessage).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        TemplateHtmlBuilder sb = new TemplateHtmlBuilder();
        sb.header("Resource Not Found", "REST interface overview");
        for (ResourceDescription resource : descriptions) {
            sb.resourcePath(resource.basePath);
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
        return Response.status(Status.NOT_FOUND).entity(sb.toString()).build();
    }

    public static class ErrorMessage {
        public String errorMessage;
        public List<ResourceDescription> existingResourcesDetails;
    }

}
