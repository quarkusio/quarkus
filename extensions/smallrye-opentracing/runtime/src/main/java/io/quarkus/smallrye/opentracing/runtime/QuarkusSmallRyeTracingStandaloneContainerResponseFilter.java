package io.quarkus.smallrye.opentracing.runtime;

import java.io.IOException;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.opentracing.contrib.jaxrs2.internal.SpanWrapper;
import io.opentracing.tag.Tags;

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
