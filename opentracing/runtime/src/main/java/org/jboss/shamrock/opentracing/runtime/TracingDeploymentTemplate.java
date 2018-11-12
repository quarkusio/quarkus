package org.jboss.shamrock.opentracing.runtime;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;

import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.opentracing.util.GlobalTracer;
import io.smallrye.opentracing.SmallRyeTracingDynamicFeature;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceHandle;
import org.jboss.shamrock.runtime.ContextObject;

/**
 * Created by bob on 8/6/18.
 */
public class TracingDeploymentTemplate {
    public void registerTracer() {
        GlobalTracer.register( new ShamrockTracer() );

    }
    public void integrateJaxrs(@ContextObject("deploymentInfo")DeploymentInfo info) {
        System.err.println( "adding integration " + info);
        info.addInitParameter("resteasy.providers", SmallRyeTracingDynamicFeature.class.getName());

        FilterInfo filterInfo = new FilterInfo("tracingFilter", SpanFinishingFilter.class, ()->{
            SpanFinishingFilter filter = new SpanFinishingFilter(GlobalTracer.get());
            return new InstanceHandle<Filter>() {
                @Override
                public Filter getInstance() {
                    System.err.println( "get instance of filter");
                    return filter;
                }

                @Override
                public void release() {
                    System.err.println( "release instance of filter");
                    // no-op
                }
            };
        });
        filterInfo.setAsyncSupported(true);
        info.addFilter(filterInfo );
        EnumSet<DispatcherType> enums = EnumSet.allOf(DispatcherType.class);
        info.addFilterUrlMapping( "tracingFilter", "*",  DispatcherType.FORWARD);
        info.addFilterUrlMapping( "tracingFilter", "*",  DispatcherType.INCLUDE);
        info.addFilterUrlMapping( "tracingFilter", "*",  DispatcherType.REQUEST);
        info.addFilterUrlMapping( "tracingFilter", "*",  DispatcherType.ASYNC);
        info.addFilterUrlMapping( "tracingFilter", "*",  DispatcherType.ERROR);
    }
}

