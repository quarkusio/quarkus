package io.quarkus.resteasy.reactive.server.servlet.runtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.undertow.vertx.VertxHttpExchange;
import io.vertx.ext.web.RoutingContext;

public class ServletRequestContextFactory implements RequestContextFactory {

    public static final ServletRequestContextFactory INSTANCE = new ServletRequestContextFactory();

    @Override
    public ResteasyReactiveRequestContext createContext(Deployment deployment, ProvidersImpl providers,
            Object context, ThreadSetupAction requestContext, ServerRestHandler[] handlerChain,
            ServerRestHandler[] abortHandlerChain) {
        io.undertow.servlet.handlers.ServletRequestContext src = (io.undertow.servlet.handlers.ServletRequestContext) context;
        return new ServletRequestContext(deployment, providers, (HttpServletRequest) src.getServletRequest(),
                (HttpServletResponse) src.getServletResponse(), requestContext, handlerChain, abortHandlerChain,
                (RoutingContext) ((VertxHttpExchange) src.getExchange().getDelegate()).getContext(), src.getExchange());
    }

    @Override
    public boolean isDefaultBlocking() {
        return true;
    }
}
