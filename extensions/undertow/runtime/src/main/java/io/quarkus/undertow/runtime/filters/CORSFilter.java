package io.quarkus.undertow.runtime.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CORSFilter implements Filter {

    // This is set in the recorder at runtime.
    // Must be static because the filter is created(deployed) at build time and runtime config is still not available
    static CORSConfig corsConfig;

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ORIGIN = "Origin";
    private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    public CORSFilter() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        Objects.requireNonNull(corsConfig, "CORS config is not set");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String origin = request.getHeader(ORIGIN);
        if (origin == null) {
            chain.doFilter(servletRequest, servletResponse);
        } else {
            String requestedMethods = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
            if (requestedMethods != null) {
                processMethods(response, requestedMethods, corsConfig.methods.orElse(requestedMethods));
            }
            String requestedHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
            if (requestedHeaders != null) {
                processHeaders(response, requestedHeaders, corsConfig.headers.orElse(requestedHeaders));
            }
            String allowedOrigins = corsConfig.origins.orElse(null);
            boolean allowsOrigin = allowedOrigins == null || allowedOrigins.contains(origin);
            if (allowsOrigin)
                response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            corsConfig.exposedHeaders.ifPresent(exposed -> response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, exposed));
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.flushBuffer();
            } else {
                chain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    private void processHeaders(HttpServletResponse response, String requestedHeaders, String allowedHeaders) {
        String validHeaders = Arrays.stream(requestedHeaders.split(","))
                .filter(allowedHeaders::contains)
                .collect(Collectors.joining(","));
        if (!validHeaders.isEmpty())
            response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, validHeaders);
    }

    private void processMethods(HttpServletResponse response, String requestedMethods, String allowedMethods) {
        String validMethods = Arrays.stream(requestedMethods.split(","))
                .filter(allowedMethods::contains)
                .collect(Collectors.joining(","));
        if (!validMethods.isEmpty())
            response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, validMethods);
    }
}
