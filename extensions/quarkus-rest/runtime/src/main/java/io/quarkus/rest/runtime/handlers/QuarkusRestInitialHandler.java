package io.quarkus.rest.runtime.handlers;

import javax.ws.rs.NotFoundException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestProviders;
import io.quarkus.rest.runtime.mapping.RequestMapper;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRestInitialHandler implements Handler<RoutingContext>, RestHandler {

    final RequestMapper<InitialMatch> mappers;
    final QuarkusRestDeployment deployment;
    final QuarkusRestProviders providers;
    final ResourceRequestInterceptorHandler preMappingHandler;
    final RestHandler[] initialChain;

    final CurrentVertxRequest currentVertxRequest;
    final ManagedContext requestContext;

    public QuarkusRestInitialHandler(RequestMapper<InitialMatch> mappers, QuarkusRestDeployment deployment,
            ResourceRequestInterceptorHandler preMappingHandler) {
        this.mappers = mappers;
        this.deployment = deployment;
        this.providers = new QuarkusRestProviders(QuarkusRestRecorder.getCurrentDeployment());
        this.preMappingHandler = preMappingHandler;
        if (preMappingHandler == null) {
            initialChain = new RestHandler[] { new MatrixParamHandler(), this };
        } else {
            initialChain = new RestHandler[] { new MatrixParamHandler(), preMappingHandler, this };
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
        public final RestHandler[] handlers;
        public final int maxPathParams;

        public InitialMatch(RestHandler[] handlers, int maxPathParams) {
            this.handlers = handlers;
            this.maxPathParams = maxPathParams;
        }
    }
}
