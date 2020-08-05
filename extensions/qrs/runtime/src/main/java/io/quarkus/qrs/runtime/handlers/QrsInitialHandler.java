package io.quarkus.qrs.runtime.handlers;

import java.util.Map;

import io.quarkus.qrs.runtime.core.QrsDeployment;
import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.mapping.RequestMapper;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QrsInitialHandler implements Handler<RoutingContext>, RestHandler {

    //TODO: this by method approach does not work for sub resource locators
    final Map<String, RequestMapper<RuntimeResource>> mappers;
    final QrsDeployment deployment;
    final ResourceRequestInterceptorHandler preMappingHandler;
    final RestHandler[] initialChain;

    public QrsInitialHandler(Map<String, RequestMapper<RuntimeResource>> mappers, QrsDeployment deployment,
            ResourceRequestInterceptorHandler preMappingHandler) {
        this.mappers = mappers;
        this.deployment = deployment;
        this.preMappingHandler = preMappingHandler;
        if (preMappingHandler == null) {
            initialChain = new RestHandler[] { this };
        } else {
            initialChain = new RestHandler[] { preMappingHandler, this };
        }
    }

    @Override
    public void handle(RoutingContext event) {
        RequestContext requestContext = new RequestContext(deployment, event, initialChain);
        requestContext.run();
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        RoutingContext event = requestContext.getContext();
        RequestMapper<RuntimeResource> mapper = mappers.get(requestContext.getMethod());
        if (mapper == null) {
            event.next();
            return;
        }
        RequestMapper.RequestMatch<RuntimeResource> target = mapper.map(event.normalisedPath());
        if (target == null) {
            event.next();
            return;
        }
        requestContext.restart(target.value);
        requestContext.setPathParamValues(target.pathParamValues);
    }
}
