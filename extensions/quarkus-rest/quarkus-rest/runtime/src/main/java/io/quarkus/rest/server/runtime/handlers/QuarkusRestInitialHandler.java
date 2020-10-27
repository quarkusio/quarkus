package io.quarkus.rest.server.runtime.handlers;

import java.util.List;

import javax.ws.rs.NotFoundException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.rest.server.runtime.QuarkusRestRecorder;
import io.quarkus.rest.server.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestProviders;
import io.quarkus.rest.server.runtime.mapping.RequestMapper;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRestInitialHandler implements Handler<RoutingContext>, ServerRestHandler {

    final RequestMapper<InitialMatch> mappers;
    final QuarkusRestDeployment deployment;
    final QuarkusRestProviders providers;
    final List<ResourceRequestFilterHandler> preMappingHandlers;
    final ServerRestHandler[] initialChain;

    final CurrentVertxRequest currentVertxRequest;
    final ManagedContext requestContext;

    public QuarkusRestInitialHandler(RequestMapper<InitialMatch> mappers, QuarkusRestDeployment deployment,
            List<ResourceRequestFilterHandler> preMappingHandlers) {
        this.mappers = mappers;
        this.deployment = deployment;
        this.providers = new QuarkusRestProviders(QuarkusRestRecorder.getCurrentDeployment());
        this.preMappingHandlers = preMappingHandlers;
        if (preMappingHandlers == null) {
            initialChain = new ServerRestHandler[] { new MatrixParamHandler(), this };
        } else {
            initialChain = new ServerRestHandler[preMappingHandlers.size() + 2];
            initialChain[0] = new MatrixParamHandler();
            for (int i = 0; i < preMappingHandlers.size(); i++) {
                initialChain[i + 1] = preMappingHandlers.get(i);
            }
            initialChain[initialChain.length - 1] = this;
        }
        this.requestContext = Arc.container().requestContext();
        this.currentVertxRequest = Arc.container().instance(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext event) {
        QuarkusRestRequestContext rq = new QuarkusRestRequestContext(deployment, providers, event, requestContext,
                currentVertxRequest,
                initialChain, deployment.getAbortHandlerChain());
        rq.run();
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        RoutingContext event = requestContext.getContext();
        RequestMapper.RequestMatch<InitialMatch> target = mappers.map(requestContext.getPathWithoutPrefix());
        if (target == null) {
            if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                // the NotFoundExceptionMapper needs access to the headers so we need to activate the scope
                requestContext.requireCDIRequestScope();
                // we want to engage the NotFoundExceptionMapper when nothing is found
                requestContext.handleException(new NotFoundException());
                return;
            } else {
                event.next();
                return;
            }
        }
        requestContext.restart(target.value.handlers);
        requestContext.setMaxPathParams(target.value.maxPathParams);
        requestContext.setRemaining(target.remaining);
        for (int i = 0; i < target.pathParamValues.length; ++i) {
            String pathParamValue = target.pathParamValues[i];
            if (pathParamValue == null) {
                break;
            }
            requestContext.setPathParamValue(i, target.pathParamValues[i]);
        }
    }

    public static class InitialMatch {
        public final ServerRestHandler[] handlers;
        public final int maxPathParams;

        public InitialMatch(ServerRestHandler[] handlers, int maxPathParams) {
            this.handlers = handlers;
            this.maxPathParams = maxPathParams;
        }
    }
}
