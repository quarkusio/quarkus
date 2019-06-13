package io.quarkus.amazon.lambda.resteasy.runtime.container;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ConfigurationBootstrap;
import org.jboss.resteasy.plugins.server.servlet.HttpRequestFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpResponseFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;
import org.jboss.resteasy.plugins.server.servlet.HttpServletResponseWrapper;
import org.jboss.resteasy.plugins.server.servlet.ServletContainerDispatcher;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.serverless.proxy.internal.testutils.Timer;

public class ResteasyLambdaFilter implements Filter, HttpRequestFactory, HttpResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ResteasyLambdaFilter.class);

    private ServletContainerDispatcher servletContainerDispatcher;

    private ServletContext servletContext;

    ResteasyLambdaFilter(ServletContext servletContext, ConfigurationBootstrap bootstrap) {
        Timer.start("RESTEASY_FILTER_CONSTRUCTOR");

        try {
            servletContainerDispatcher = new ServletContainerDispatcher();
            servletContainerDispatcher.init(servletContext, bootstrap, this, this);
        } catch (ServletException e) {
            throw new IllegalStateException("Unable to initialize the RESTEasy dispatcher", e);
        }

        Timer.stop("RESTEASY_FILTER_CONSTRUCTOR");
    }

    @Override
    public void init(FilterConfig filterConfig) {
        LOG.info("Initialize RESTEasy filter");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        Timer.start("RESTEASY_FILTER_DOFILTER");

        assert servletRequest instanceof HttpServletRequest;
        assert servletResponse instanceof HttpServletResponse;

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        servletContainerDispatcher.service(httpServletRequest.getMethod(), httpServletRequest, httpServletResponse, true);

        Timer.stop("RESTEASY_FILTER_DOFILTER");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public HttpResponse createResteasyHttpResponse(HttpServletResponse response) {
        return new HttpServletResponseWrapper(response, servletContainerDispatcher.getDispatcher().getProviderFactory());
    }

    @Override
    public HttpRequest createResteasyHttpRequest(String httpMethod, HttpServletRequest request, ResteasyHttpHeaders headers,
            ResteasyUriInfo uriInfo, HttpResponse theResponse, HttpServletResponse response) {
        // in the Jersey implementation, there is code to strip the base path from the URI
        // we don't need it here as we use directly the AwsProxyHttpServletRequest
        // which has the contextPath properly configured.

        return new HttpServletInputMessage(request, response, servletContext, theResponse, headers, uriInfo,
                httpMethod.toUpperCase(),
                (SynchronousDispatcher) servletContainerDispatcher.getDispatcher());
    }

    @Override
    public void destroy() {
        LOG.info("Destroy RESTEasy filter");
        servletContainerDispatcher.destroy();
    }
}
