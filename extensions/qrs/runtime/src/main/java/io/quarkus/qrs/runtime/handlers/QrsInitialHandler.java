package io.quarkus.qrs.runtime.handlers;

import java.util.Map;

import io.quarkus.qrs.runtime.core.ExceptionMapping;
import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.mapping.RequestMapper;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QrsInitialHandler implements Handler<RoutingContext> {

    //TODO: this by method approach does not work for sub resource locators
    final Map<String, RequestMapper<RuntimeResource>> mappers;
    final ExceptionMapping exceptionMapping;
    final Serialisers serialisers;

    public QrsInitialHandler(Map<String, RequestMapper<RuntimeResource>> mappers, ExceptionMapping exceptionMapping,
            Serialisers serialisers) {
        this.mappers = mappers;
        this.exceptionMapping = exceptionMapping;
        this.serialisers = serialisers;
    }

    @Override
    public void handle(RoutingContext event) {
        RequestMapper<RuntimeResource> mapper = mappers.get(event.request().method().name());
        if (mapper == null) {
            event.next();
            return;
        }
        RequestMapper.RequestMatch<RuntimeResource> target = mapper.map(event.normalisedPath());
        if (target == null) {
            event.next();
            return;
        }
        RequestContext requestContext = new RequestContext(event, target.value, exceptionMapping, serialisers);
        requestContext.setPathParamValues(target.pathParamValues);
        requestContext.run();
    }
}
