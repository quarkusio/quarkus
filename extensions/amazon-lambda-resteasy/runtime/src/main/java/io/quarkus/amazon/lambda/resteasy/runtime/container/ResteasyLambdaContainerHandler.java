package io.quarkus.amazon.lambda.resteasy.runtime.container;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.jboss.resteasy.plugins.server.servlet.ServletBootstrap;

import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

public class ResteasyLambdaContainerHandler<RequestType, ResponseType> extends
        AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

    private ResteasyLambdaFilter resteasyFilter;

    public static ResteasyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler(
            Map<String, String> initParameters) {
        ResteasyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> newHandler = new ResteasyLambdaContainerHandler<>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                initParameters);
        newHandler.initialize();
        return newHandler;
    }

    public ResteasyLambdaContainerHandler(Class<RequestType> requestTypeClass,
            Class<ResponseType> responseTypeClass,
            RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
            SecurityContextWriter<RequestType> securityContextWriter,
            ExceptionHandler<ResponseType> exceptionHandler,
            Map<String, String> initParameters) {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start("RESTEASY_CONTAINER_CONSTRUCTOR");

        this.resteasyFilter = new ResteasyLambdaFilter(getServletContext(),
                new LambdaServletBootstrap(new ServletConfigImpl(getServletContext(), initParameters)));

        Timer.stop("RESTEASY_CONTAINER_CONSTRUCTOR");
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest httpServletRequest, AwsHttpServletResponse httpServletResponse,
            Context lambdaContext)
            throws Exception {
        Timer.start("RESTEASY_HANDLE_REQUEST");

        httpServletRequest.setServletContext(getServletContext());

        doFilter(httpServletRequest, httpServletResponse, null);
        Timer.stop("RESTEASY_HANDLE_REQUEST");
    }

    @Override
    public void initialize() {
        Timer.start("RESTEASY_COLD_START_INIT");

        // manually add the spark filter to the chain. This should be the last one and match all uris
        FilterRegistration.Dynamic resteasyFilterReg = getServletContext().addFilter("RESTEasyFilter", resteasyFilter);
        resteasyFilterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

        Timer.stop("RESTEASY_COLD_START_INIT");
    }

    private static class LambdaServletBootstrap extends ServletBootstrap {

        public LambdaServletBootstrap(ServletConfig config) {
            super(config);
        }

        @Override
        public String getParameter(String name) {
            return super.getInitParameter(name);
        }
    }

    private static class ServletConfigImpl implements ServletConfig {

        private static final String SERVLET_NAME = "RESTEasy Lambda Servlet";

        private final ServletContext servletContext;

        private final Map<String, String> initParameters;

        private ServletConfigImpl(ServletContext servletContext, Map<String, String> initParameters) {
            this.servletContext = servletContext;
            this.initParameters = Collections.unmodifiableMap(initParameters);
        }

        @Override
        public String getServletName() {
            return SERVLET_NAME;
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Init parameter name cannot be null.");
            }
            return initParameters.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return new IteratorEnumeration<>(initParameters.keySet().iterator());
        }
    }

    public static class IteratorEnumeration<T> implements Enumeration<T> {

        private final Iterator<T> iterator;

        public IteratorEnumeration(final Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }
    }
}
