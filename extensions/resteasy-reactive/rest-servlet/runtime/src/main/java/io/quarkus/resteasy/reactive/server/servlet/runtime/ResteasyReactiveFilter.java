package io.quarkus.resteasy.reactive.server.servlet.runtime;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;

import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;

public class ResteasyReactiveFilter extends HttpFilter {

    private final RestInitialHandler initialHandler;

    public ResteasyReactiveFilter(Deployment deployment) {
        this.initialHandler = new RestInitialHandler(deployment);
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = req;
        while (request instanceof HttpServletRequestWrapper) {
            request = (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
        }
        initialHandler.beginProcessing(
                ((HttpServletRequestImpl) request).getExchange().getAttachment(ServletRequestContext.ATTACHMENT_KEY));
    }
}
