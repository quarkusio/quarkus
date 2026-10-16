package org.jboss.resteasy.reactive.server.handlers;

import static org.jboss.resteasy.reactive.server.handlers.HandlerMediaTypeUtil.*;
import static org.jboss.resteasy.reactive.server.handlers.HandlerMediaTypeUtil.validateConsumes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.jaxrs.ResponseBuilderImpl;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class ClassRoutingHandler implements ServerRestHandler {

    private final Map<String, RequestMapper<RuntimeResource>> mappers;
    private final int parameterOffset;
    final boolean servletPresent;

    public ClassRoutingHandler(Map<String, RequestMapper<RuntimeResource>> mappers, int parameterOffset,
            boolean servletPresent) {
        this.mappers = mappers;
        this.parameterOffset = parameterOffset;
        this.servletPresent = servletPresent;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        RequestMapper<RuntimeResource> mapper = mappers.get(requestContext.getMethod());
        if (mapper == null) {
            String requestMethod = requestContext.getMethod();
            if (requestMethod.equals(HttpMethod.HEAD)) {
                mapper = mappers.get(HttpMethod.GET);
            } else if (requestMethod.equals(HttpMethod.OPTIONS)) {
                Set<String> allowedMethods = new HashSet<>();
                for (String method : mappers.keySet()) {
                    if (method == null) {
                        continue;
                    }
                    allowedMethods.add(method);
                }
                allowedMethods.add(HttpMethod.OPTIONS);
                allowedMethods.add(HttpMethod.HEAD);
                requestContext.abortWith(Response.ok().allow(allowedMethods).build());
                return;
            }
            if (mapper == null) {
                mapper = mappers.get(null);
            }
            if (mapper == null) {
                if (requestContext.restartWithNextInitialMatch()) {
                    return;
                }
                // The idea here is to check if any of the mappers of the class could map the request - if the HTTP Method were correct
                String remaining = getRemaining(requestContext);
                for (RequestMapper<RuntimeResource> existingMapper : mappers.values()) {
                    if (existingMapper.map(remaining) != null) {
                        throw new NotAllowedException(
                                new ResponseBuilderImpl().status(Response.Status.METHOD_NOT_ALLOWED).build());
                    }
                }
                throwNotFound(requestContext);
                return;
            }
        }
        String remaining = getRemaining(requestContext);
        RequestMapper.RequestMatch<RuntimeResource> target = mapper.map(remaining);
        if (target == null) {
            if (requestContext.getMethod().equals(HttpMethod.HEAD)) {
                mapper = mappers.get(HttpMethod.GET);
                if (mapper != null) {
                    target = mapper.map(remaining);
                }
            }

            if (target == null) {
                if (requestContext.restartWithNextInitialMatch()) {
                    return;
                }
                // The idea here is to check if any of the mappers of the class could map the request - if the HTTP Method were correct
                for (Map.Entry<String, RequestMapper<RuntimeResource>> entry : mappers.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    if (entry.getKey().equals(requestContext.getMethod())) {
                        continue;
                    }
                    if (entry.getValue().map(remaining) != null) {
                        throw new NotAllowedException(
                                new ResponseBuilderImpl().status(Response.Status.METHOD_NOT_ALLOWED).build());
                    }
                }
                throwNotFound(requestContext);
                return;
            }
        }

        validateConsumes(target, requestContext);

        validateProduces(target, requestContext);

        requestContext.restart(target.value);
        requestContext.setRemaining(target.remaining);
        for (int i = 0; i < target.pathParamValues.length; ++i) {
            String pathParamValue = target.pathParamValues[i];
            if (pathParamValue == null) {
                break;
            }
            requestContext.setPathParamValue(i + parameterOffset, pathParamValue);
        }
    }

    private void throwNotFound(ResteasyReactiveRequestContext requestContext) {
        ProvidersImpl providers = requestContext.getProviders();
        ExceptionMapper<NotFoundException> exceptionMapper = providers.getExceptionMapper(NotFoundException.class);

        if (exceptionMapper == null || servletPresent) {
            if (requestContext.resumeExternalProcessing()) {
                return;
            }
        }
        // the exception mapper needs access to request scoped beans, so make sure we have the context
        requestContext.requireCDIRequestScope();
        throw new NotFoundException("Unable to find matching target resource method");

    }

    private String getRemaining(ResteasyReactiveRequestContext requestContext) {
        return requestContext.getRemaining().isEmpty() ? "/" : requestContext.getRemaining();
    }

    public Map<String, RequestMapper<RuntimeResource>> getMappers() {
        return mappers;
    }
}
