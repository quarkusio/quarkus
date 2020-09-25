package io.quarkus.rest.runtime.handlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponseBuilder;
import io.quarkus.rest.runtime.mapping.RequestMapper;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.util.MediaTypeHelper;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class ClassRoutingHandler implements RestHandler {
    final Map<String, RequestMapper<RuntimeResource>> mappers;
    private final int parameterOffset;

    public ClassRoutingHandler(Map<String, RequestMapper<RuntimeResource>> mappers, int parameterOffset) {
        this.mappers = mappers;
        this.parameterOffset = parameterOffset;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        RoutingContext event = requestContext.getContext();
        RequestMapper<RuntimeResource> mapper = mappers.get(requestContext.getMethod());
        if (mapper == null) {
            mapper = mappers.get(null);
            if (mapper == null) {
                if (requestContext.getMethod().equals(HttpMethod.HEAD.name())) {
                    mapper = mappers.get(HttpMethod.GET.name());
                } else if (requestContext.getMethod().equals(HttpMethod.OPTIONS.name())) {
                    //just send back 200
                    event.response().end();
                    return;
                }
                if (mapper == null) {
                    // The idea here is to check if any of the mappers of the class could map the request - if the HTTP Method were correct
                    String remaining = getRemaining(requestContext);
                    for (RequestMapper<RuntimeResource> existingMapper : mappers.values()) {
                        if (existingMapper.map(remaining) != null) {
                            throw new NotAllowedException(
                                    new QuarkusRestResponseBuilder().status(Response.Status.METHOD_NOT_ALLOWED).build());
                        }
                    }
                    throw new NotFoundException();
                }
            }
        }
        String remaining = getRemaining(requestContext);
        RequestMapper.RequestMatch<RuntimeResource> target = mapper.map(remaining);
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
                            new QuarkusRestResponseBuilder().status(Response.Status.METHOD_NOT_ALLOWED).build());
                }
            }
            throw new NotFoundException();
        }

        // according to the spec we need to return HTTP 415 when content-type header doesn't match what is specified in @Consumes
        if (!target.value.getConsumes().isEmpty()) {
            String contentType = requestContext.getContext().request().headers().get(HttpHeaders.CONTENT_TYPE);
            if (contentType != null) {
                if (MediaTypeHelper.getBestMatch(
                        target.value.getConsumes(),
                        Collections.singletonList(MediaType.valueOf(contentType))) == null) {
                    throw new NotSupportedException();
                }
            }
        }
        // according to the spec we need to return HTTP 406 when Accept header doesn't match what is specified in @Produces
        if (target.value.getProduces() != null) {
            String accepts = requestContext.getContext().request().headers().get(HttpHeaders.ACCEPT);
            if (accepts != null) {
                if (MediaTypeHelper.getBestMatch(Arrays.asList(target.value.getProduces().getSortedMediaTypes()),
                        Collections.singletonList(MediaType.valueOf(accepts))) == null) {
                    throw new NotAcceptableException();
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

    private String getRemaining(QuarkusRestRequestContext requestContext) {
        return requestContext.getRemaining().isEmpty() ? "/" : requestContext.getRemaining();
    }
}
