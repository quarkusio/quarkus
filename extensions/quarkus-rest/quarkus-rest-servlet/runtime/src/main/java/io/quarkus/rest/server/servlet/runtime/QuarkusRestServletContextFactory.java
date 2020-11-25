package io.quarkus.rest.server.servlet.runtime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;

import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.vertx.VertxHttpExchange;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRestServletContextFactory implements RequestContextFactory {

    public static final QuarkusRestServletContextFactory INSTANCE = new QuarkusRestServletContextFactory();

    @Override
    public ResteasyReactiveRequestContext createContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers,
            Object context, ThreadSetupAction requestContext, ServerRestHandler[] handlerChain,
            ServerRestHandler[] abortHandlerChain) {
        ServletRequestContext src = (ServletRequestContext) context;
        return new QuarkusServletRequestContext(deployment, providers, (HttpServletRequest) src.getServletRequest(),
                (HttpServletResponse) src.getServletResponse(), requestContext, handlerChain, abortHandlerChain,
                (RoutingContext) ((VertxHttpExchange) src.getExchange().getDelegate()).getContext());
    }
}
