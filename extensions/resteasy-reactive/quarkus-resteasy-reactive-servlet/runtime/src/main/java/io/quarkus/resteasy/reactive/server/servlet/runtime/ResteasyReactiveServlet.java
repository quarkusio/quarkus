package io.quarkus.resteasy.reactive.server.servlet.runtime;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;

import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;

public class ResteasyReactiveServlet extends HttpServlet {

    private final RestInitialHandler initialHandler;

    public ResteasyReactiveServlet(Deployment deployment) {
        this.initialHandler = new RestInitialHandler(deployment);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpServletRequest request = req;
        while (request instanceof HttpServletRequestWrapper) {
            request = (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
        }
        initialHandler.beginProcessing(
                ((HttpServletRequestImpl) request).getExchange().getAttachment(ServletRequestContext.ATTACHMENT_KEY));
    }
}
