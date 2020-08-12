package io.quarkus.qrs.runtime.handlers;

import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.qrs.runtime.core.QrsDeployment;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.mapping.RequestMapper;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QrsInitialHandler implements Handler<RoutingContext>, RestHandler {

    final RequestMapper<RestHandler[]> mappers;
    final QrsDeployment deployment;
    final ResourceRequestInterceptorHandler preMappingHandler;
    final RestHandler[] initialChain;

    final CurrentVertxRequest currentVertxRequest;
    final ManagedContext requestContext;

    public QrsInitialHandler(RequestMapper<RestHandler[]> mappers, QrsDeployment deployment,
            ResourceRequestInterceptorHandler preMappingHandler) {
        this.mappers = mappers;
        this.deployment = deployment;
        this.preMappingHandler = preMappingHandler;
        if (preMappingHandler == null) {
            initialChain = new RestHandler[] { this };
        } else {
            initialChain = new RestHandler[] { preMappingHandler, this };
        }
        this.requestContext = Arc.container().requestContext();
        this.currentVertxRequest = Arc.container().instance(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext event) {
        QrsRequestContext rq = new QrsRequestContext(deployment, event, requestContext, currentVertxRequest, initialChain);
        rq.run();
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        RoutingContext event = requestContext.getContext();
        RequestMapper.RequestMatch<RestHandler[]> target = mappers.map(event.normalisedPath());
        if (target == null) {
            event.next();
            return;
        }
        requestContext.restart(target.value);
        requestContext.setRemaining(target.remaining);
        requestContext.setPathParamValues(target.pathParamValues);
    }
}
