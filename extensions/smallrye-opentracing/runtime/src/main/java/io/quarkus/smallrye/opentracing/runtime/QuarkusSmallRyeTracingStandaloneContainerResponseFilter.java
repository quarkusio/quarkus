package io.quarkus.smallrye.opentracing.runtime;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.opentracing.tag.Tags;
import io.smallrye.opentracing.contrib.jaxrs2.internal.SpanWrapper;

@Provider
// We must close the span after everything else has finished
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class QuarkusSmallRyeTracingStandaloneContainerResponseFilter implements WriterInterceptor {

    @ServerResponseFilter(priority = Priorities.HEADER_DECORATOR - 1) // this needs to be executed after ServerTracingFilter
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext, Throwable t) {
        Object wrapperObj = requestContext.getProperty(SpanWrapper.PROPERTY_NAME);
        if (!(wrapperObj instanceof SpanWrapper)) {
            return;
        }
        SpanWrapper wrapper = (SpanWrapper) wrapperObj;
        Tags.HTTP_STATUS.set(wrapper.get(), responseContext.getStatus());
        if (t != null) {
            FilterUtil.addExceptionLogs(wrapper.get(), t);
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext wic) throws IOException {
        try {
            wic.proceed();
        } finally {
            Object wrapperObj = wic.getProperty(SpanWrapper.PROPERTY_NAME);
            if (wrapperObj instanceof SpanWrapper) {
                SpanWrapper wrapper = (SpanWrapper) wrapperObj;
                wrapper.getScope().close();
                wrapper.finish();
            }
        }
    }
}
