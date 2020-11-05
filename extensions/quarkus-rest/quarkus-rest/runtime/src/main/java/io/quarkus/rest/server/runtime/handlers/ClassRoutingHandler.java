package io.quarkus.rest.server.runtime.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.runtime.headers.MediaTypeHeaderDelegate;
import org.jboss.resteasy.reactive.common.runtime.util.MediaTypeHelper;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestServerResponseBuilder;
import io.quarkus.rest.server.runtime.mapping.RequestMapper;
import io.quarkus.rest.server.runtime.mapping.RuntimeResource;
import io.quarkus.runtime.LaunchMode;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;

public class ClassRoutingHandler implements ServerRestHandler {
    private final Map<String, RequestMapper<RuntimeResource>> mappers;
    private final int parameterOffset;

    public ClassRoutingHandler(Map<String, RequestMapper<RuntimeResource>> mappers, int parameterOffset) {
        this.mappers = mappers;
        this.parameterOffset = parameterOffset;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        RequestMapper<RuntimeResource> mapper = mappers.get(requestContext.getMethod());
        if (mapper == null) {
            String requestMethod = requestContext.getMethod();
            if (requestMethod.equals(HttpMethod.HEAD.name())) {
                mapper = mappers.get(HttpMethod.GET.name());
            } else if (requestMethod.equals(HttpMethod.OPTIONS.name())) {
                Set<String> allowedMethods = new HashSet<>();
                for (String method : mappers.keySet()) {
                    if (method == null) {
                        continue;
                    }
                    allowedMethods.add(method);
                }
                allowedMethods.add(HttpMethod.OPTIONS.name());
                allowedMethods.add(HttpMethod.HEAD.name());
                requestContext.getHttpServerResponse().putHeader(HttpHeaders.ALLOW, allowedMethods).end();
                return;
            }
            if (mapper == null) {
                mapper = mappers.get(null);
            }
            if (mapper == null) {
                // The idea here is to check if any of the mappers of the class could map the request - if the HTTP Method were correct
                String remaining = getRemaining(requestContext);
                for (RequestMapper<RuntimeResource> existingMapper : mappers.values()) {
                    if (existingMapper.map(remaining) != null) {
                        throw new NotAllowedException(
                                new QuarkusRestServerResponseBuilder().status(Response.Status.METHOD_NOT_ALLOWED).build());
                    }
                }
                throwNotFound(requestContext);
                return;
            }
        }
        String remaining = getRemaining(requestContext);
        RequestMapper.RequestMatch<RuntimeResource> target = mapper.map(remaining);
        if (target == null) {
            if (requestContext.getMethod().equals(HttpMethod.HEAD.name())) {
                mapper = mappers.get(HttpMethod.GET.name());
                if (mapper != null) {
                    target = mapper.map(remaining);
                }
            }

            if (target == null) {
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
                                new QuarkusRestServerResponseBuilder().status(Response.Status.METHOD_NOT_ALLOWED).build());
                    }
                }
                throwNotFound(requestContext);
                return;
            }
        }

        // use vert.x headers wherever we need header checking because we really don't want to
        // copy headers as it's a performance killer
        MultiMap vertxHeaders = requestContext.getContext().request().headers();

        // according to the spec we need to return HTTP 415 when content-type header doesn't match what is specified in @Consumes

        if (!target.value.getConsumes().isEmpty()) {
            String contentType = vertxHeaders.get(HttpHeaders.CONTENT_TYPE);
            if (contentType != null) {
                if (MediaTypeHelper.getFirstMatch(
                        target.value.getConsumes(),
                        Collections.singletonList(MediaType.valueOf(contentType))) == null) {
                    throw new NotSupportedException();
                }
            }
        }
        // according to the spec we need to return HTTP 406 when Accept header doesn't match what is specified in @Produces
        if (target.value.getProduces() != null) {
            String accepts = vertxHeaders.get(HttpHeaders.ACCEPT);
            if ((accepts != null) && !accepts.equals(MediaType.WILDCARD)) {
                if (!accepts.contains(",") && target.value.getProduces().getSortedMediaTypes().length == 1) { // the point of this branch is to eliminate the list creation and sorting
                    MediaType acceptsMediaType = MediaType.valueOf(accepts.trim());
                    MediaType providedMediaType = target.value.getProduces().getSortedMediaTypes()[0];
                    if (!providedMediaType.isCompatible(acceptsMediaType)) {
                        throw new NotAcceptableException();
                    }
                } else {
                    // don't use any of the JAX-RS stuff from the various MediaType helper as we want to be as performant as possible
                    List<MediaType> acceptsMediaTypes;
                    if (accepts.contains(",")) {
                        String[] parts = accepts.split(",");
                        acceptsMediaTypes = new ArrayList<>(parts.length);
                        for (int i = 0; i < parts.length; i++) {
                            String part = parts[i];
                            acceptsMediaTypes.add(toMediaType(part.trim()));
                        }
                    } else {
                        acceptsMediaTypes = Collections.singletonList(toMediaType(accepts));
                    }
                    if (MediaTypeHelper.getFirstMatch(Arrays.asList(target.value.getProduces().getSortedMediaTypes()),
                            acceptsMediaTypes) == null) {
                        throw new NotAcceptableException();
                    }
                }
            }
        }

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

    private MediaType toMediaType(String mediaTypeStr) {
        return MediaTypeHeaderDelegate.parse(mediaTypeStr);
    }

    private void throwNotFound(QuarkusRestRequestContext requestContext) {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            // the NotFoundExceptionHandler needs access to request scoped beans, so make sure we have the context
            requestContext.requireCDIRequestScope();
        }
        throw new NotFoundException();
    }

    private String getRemaining(QuarkusRestRequestContext requestContext) {
        return requestContext.getRemaining().isEmpty() ? "/" : requestContext.getRemaining();
    }

    public Map<String, RequestMapper<RuntimeResource>> getMappers() {
        return mappers;
    }
}
