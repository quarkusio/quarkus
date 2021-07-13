package io.quarkus.micrometer.runtime.binder.vertx;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.vertx.ext.web.RoutingContext;

/**
 * This needs to run before the Vert.x layer sees the "end" of the response.
 * HttpFilter meets that requirement. Filter does not.
 */
public class VertxMeterBinderUndertowServletFilter extends HttpFilter {

    @Inject
    RoutingContext routingContext;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        try {
            chain.doFilter(req, res);
        } finally {
            // Fallback. Only set if not already set by something smarter
            HttpRequestMetric metric = HttpRequestMetric.getRequestMetric(routingContext);
            metric.setTemplatePath(req.getServletPath());
        }
    }
}