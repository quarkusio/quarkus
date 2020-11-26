package org.jboss.resteasy.reactive.server.handlers;

import java.util.List;
import javax.ws.rs.NotFoundException;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class QuarkusRestInitialHandler implements ServerRestHandler {

    final RequestMapper<InitialMatch> mappers;
    final Deployment deployment;
    final ProvidersImpl providers;
    final List<ResourceRequestFilterHandler> preMappingHandlers;
    final ServerRestHandler[] initialChain;

    final ThreadSetupAction requestContext;
    final RequestContextFactory requestContextFactory;

    public QuarkusRestInitialHandler(Deployment deployment) {
        this.mappers = new RequestMapper<>(deployment.getClassMappers());
        this.deployment = deployment;
        this.providers = new ProvidersImpl(deployment);
        this.preMappingHandlers = deployment.getPreMatchHandlers();
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
        this.requestContext = deployment.getThreadSetupAction();
        this.requestContextFactory = deployment.getRequestContextFactory();
    }

    public void beginProcessing(Object extenalHttpContext) {
        ResteasyReactiveRequestContext rq = requestContextFactory.createContext(deployment, providers, extenalHttpContext,
                requestContext,
                initialChain, deployment.getAbortHandlerChain());
        rq.run();
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        RequestMapper.RequestMatch<InitialMatch> target = mappers.map(requestContext.getPathWithoutPrefix());
        if (target == null) {
            // the NotFoundExceptionMapper needs access to the headers so we need to activate the scope
            requestContext.requireCDIRequestScope();
            // we want to engage the NotFoundExceptionMapper when nothing is found
            requestContext.handleException(new NotFoundException());
            return;
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
