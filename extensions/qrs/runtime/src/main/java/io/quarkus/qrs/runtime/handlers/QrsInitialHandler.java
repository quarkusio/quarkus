package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.mapping.RequestMapper;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QrsInitialHandler implements Handler<RoutingContext> {

    final RequestMapper<RuntimeResource> mapper;

    public QrsInitialHandler(RequestMapper<RuntimeResource> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(RoutingContext event) {
        RequestMapper.RequestMatch<RuntimeResource> target = mapper.map(event.normalisedPath());
        if (target == null) {
            event.next();
            return;
        }
        RequestContext requestContext = new RequestContext(event, target.value);
        requestContext.setPathParamValues(target.pathParamValues);
        requestContext.run();
    }
}
