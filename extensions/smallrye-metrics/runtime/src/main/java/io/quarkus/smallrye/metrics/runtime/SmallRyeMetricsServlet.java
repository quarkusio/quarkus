package io.quarkus.smallrye.metrics.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.metrics.MetricsRequestHandler;

@WebServlet
public class SmallRyeMetricsServlet extends HttpServlet {

    @Inject
    MetricsRequestHandler metricsHandler;

    @ConfigProperty(name = "quarkus.servlet.context-path", defaultValue = "/")
    String appContextPath;

    // full path where the metrics endpoint resides, by default it's /metrics, but can be /appContextPath/anything
    private String contextPath;

    @Override
    public void init() {
        String metricsPath = getInitParameter("metrics.path");
        // add leading / if there isn't one
        String metricPathSanitized = metricsPath.startsWith("/") ? metricsPath : "/" + metricsPath;
        // strip off trailing / if there is
        String appContextPathSanitized = appContextPath.endsWith("/") ? appContextPath.substring(0, appContextPath.length() - 1)
                : appContextPath;
        contextPath = appContextPathSanitized + metricPathSanitized;
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        Stream<String> acceptHeaders = Collections.list(request.getHeaders("Accept")).stream();

        metricsHandler.handleRequest(requestPath, contextPath, method, acceptHeaders, (status, message, headers) -> {
            headers.forEach(response::addHeader);
            response.setStatus(status);
            response.getWriter().write(message);
        });
    }
}
