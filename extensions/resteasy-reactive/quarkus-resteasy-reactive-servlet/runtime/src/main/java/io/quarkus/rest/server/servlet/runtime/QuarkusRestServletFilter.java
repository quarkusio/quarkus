package io.quarkus.rest.server.servlet.runtime;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.QuarkusRestInitialHandler;

import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;

public class QuarkusRestServletFilter extends HttpFilter {

    private final QuarkusRestInitialHandler initialHandler;

    public QuarkusRestServletFilter(Deployment deployment) {
        this.initialHandler = new QuarkusRestInitialHandler(deployment);
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
