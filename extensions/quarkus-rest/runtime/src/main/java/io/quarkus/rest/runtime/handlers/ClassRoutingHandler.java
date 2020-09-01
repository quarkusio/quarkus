package io.quarkus.rest.runtime.handlers;

import java.util.Map;

import javax.ws.rs.NotFoundException;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.mapping.RequestMapper;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
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
                    throw new NotFoundException();
                }
            }
        }
        RequestMapper.RequestMatch<RuntimeResource> target = mapper
                .map(requestContext.getRemaining().isEmpty() ? "/" : requestContext.getRemaining());
        if (target == null) {
            throw new NotFoundException();
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
}
