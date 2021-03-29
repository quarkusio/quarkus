package io.quarkus.smallrye.opentracing.runtime;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.opentracing.contrib.jaxrs2.internal.SpanWrapper;
import io.opentracing.tag.Tags;

public class QuarkusSmallRyeTracingStandaloneContainerResponseFilter {

    @ServerResponseFilter(priority = Priorities.HEADER_DECORATOR - 1) // this needs to be executed after ServerTracingFilter
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext, Throwable t) {
        Object wrapperObj = requestContext.getProperty(SpanWrapper.PROPERTY_NAME);
        if (!(wrapperObj instanceof SpanWrapper)) {
            return;
        }
        SpanWrapper wrapper = (SpanWrapper) wrapperObj;
        wrapper.getScope().close();
        Tags.HTTP_STATUS.set(wrapper.get(), responseContext.getStatus());
        if (t != null) {
            FilterUtil.addExceptionLogs(wrapper.get(), t);
        }
        wrapper.finish();
    }
}
