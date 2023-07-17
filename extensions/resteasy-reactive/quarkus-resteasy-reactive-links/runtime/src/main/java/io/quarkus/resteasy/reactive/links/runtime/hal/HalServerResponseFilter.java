package io.quarkus.resteasy.reactive.links.runtime.hal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;

import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalService;

public class HalServerResponseFilter {

    private static final String COLLECTION_NAME = "items";

    private final HalService service;

    @Inject
    public HalServerResponseFilter(HalService service) {
        this.service = service;
    }

    @ServerResponseFilter
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext, Throwable t) {
        if (t == null) {
            Object entity = responseContext.getEntity();
            if (isHttpStatusSuccessful(responseContext.getStatusInfo())
                    && acceptsHalMediaType(requestContext)
                    && canEntityBeProcessed(entity)) {
                if (entity instanceof Collection) {
                    responseContext.setEntity(service.toHalCollectionWrapper((Collection<Object>) entity, COLLECTION_NAME,
                            findEntityClass()));
                } else {
                    responseContext.setEntity(service.toHalWrapper(entity));
                }
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
        return acceptMediaType.contains(RestMediaType.APPLICATION_HAL_JSON);
    }

    private Class<?> findEntityClass() {
        Type entityType = CurrentRequestManager.get().getTarget().getReturnType();
        if (entityType instanceof ParameterizedType) {
            // we can resolve the entity class from the param type
            Type itemEntityType = ((ParameterizedType) entityType).getActualTypeArguments()[0];
            if (itemEntityType instanceof Class) {
                return (Class<?>) itemEntityType;
            }
        }

        return null;
    }

}
