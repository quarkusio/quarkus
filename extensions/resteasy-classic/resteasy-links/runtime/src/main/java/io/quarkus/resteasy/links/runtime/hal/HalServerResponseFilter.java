package io.quarkus.resteasy.links.runtime.hal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.hal.HalEntityWrapper;

@Provider
public class HalServerResponseFilter implements ContainerResponseFilter {

    private static final String APPLICATION_HAL_JSON = "application/hal+json";
    private static final String COLLECTION_NAME = "items";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object entity = responseContext.getEntity();
        if (isHttpStatusSuccessful(responseContext.getStatusInfo())
                && acceptsHalMediaType(requestContext)
                && canEntityBeProcessed(entity)) {
            ResteasyHalService service = Arc.container().instance(ResteasyHalService.class).get();
            if (entity instanceof Collection) {
                responseContext.setEntity(service.toHalCollectionWrapper((Collection<Object>) entity,
                        COLLECTION_NAME, findEntityClass(requestContext, responseContext.getEntityType())));
            } else {
                responseContext.setEntity(service.toHalWrapper(entity));
            }
        }
    }

    private boolean canEntityBeProcessed(Object entity) {
        return entity != null
                && !(entity instanceof String)
                && !(entity instanceof HalEntityWrapper || entity instanceof HalCollectionWrapper);
    }

    private boolean isHttpStatusSuccessful(Response.StatusType statusInfo) {
        return Response.Status.Family.SUCCESSFUL.equals(statusInfo.getFamily());
    }

    private boolean acceptsHalMediaType(ContainerRequestContext requestContext) {
        List<String> acceptMediaType = requestContext.getAcceptableMediaTypes().stream().map(MediaType::toString).collect(
                Collectors.toList());
        return acceptMediaType.contains(APPLICATION_HAL_JSON);
    }

    private Class<?> findEntityClass(ContainerRequestContext requestContext, Type entityType) {
        if (entityType instanceof ParameterizedType) {
            // we can resolve the entity class from the param type
            return (Class<?>) ((ParameterizedType) entityType).getActualTypeArguments()[0];
        }

        return null;
    }
}
