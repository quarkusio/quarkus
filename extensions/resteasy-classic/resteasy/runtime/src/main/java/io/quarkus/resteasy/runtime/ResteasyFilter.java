package io.quarkus.resteasy.runtime;

import java.io.IOException;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

/**
 * A filter that will be mapped to the default servlet. At first content will attempt to be served from the
 * default servlet, and if it fails then a REST response will be attempted
 */
public class ResteasyFilter extends Filter30Dispatcher {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (request.getMethod().equals("GET") || request.getMethod().equals("HEAD") || isCORSPreflightRequest(request)) {
            //we only serve get requests from the default servlet and CORS preflight requests
            filterChain.doFilter(servletRequest, new ResteasyResponseWrapper(response, request));
        } else {
            CurrentVertxRequest currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
            ContextUtil.pushContext(currentVertxRequest.getCurrent());
            servletContainerDispatcher.service(request.getMethod(), request, response, true);
        }
    }

    private boolean isCORSPreflightRequest(HttpServletRequest request) {
        return request.getMethod().equals("OPTIONS")
                && request.getHeader("Origin") != null
                && request.getHeader("Access-Control-Request-Method") != null
                && request.getHeader("Access-Control-Request-Headers") != null;
    }

    private class ResteasyResponseWrapper extends HttpServletResponseWrapper {

        final HttpServletRequest request;
        final HttpServletResponse response;

        public ResteasyResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
            this.response = response;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            if (sc == 404 || sc == 403) {

                service();
            } else {
                super.sendError(sc, msg);
            }
        }

        protected void service() throws IOException {
            CurrentVertxRequest currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
            ContextUtil.pushContext(currentVertxRequest.getCurrent());
            servletContainerDispatcher.service(request.getMethod(), request, response, true);
        }

        @Override
        public void sendError(int sc) throws IOException {
            if (sc == 404 || sc == 403) {
                service();
            } else {
                super.sendError(sc);
            }
        }
    }
}
