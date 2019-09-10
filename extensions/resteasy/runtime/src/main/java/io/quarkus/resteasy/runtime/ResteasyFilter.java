package io.quarkus.resteasy.runtime;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;

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
                servletContainerDispatcher.service(request.getMethod(), request, response, true);
            } else {
                super.sendError(sc, msg);
            }
        }

        @Override
        public void sendError(int sc) throws IOException {
            if (sc == 404 || sc == 403) {
                servletContainerDispatcher.service(request.getMethod(), request, response, true);
            } else {
                super.sendError(sc);
            }
        }
    }
}