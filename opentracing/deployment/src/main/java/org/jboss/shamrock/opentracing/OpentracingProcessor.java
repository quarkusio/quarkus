package org.jboss.shamrock.opentracing;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.servlet.DispatcherType;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.ReflectiveMethodBuildItem;
import org.jboss.shamrock.jaxrs.JaxrsProviderBuildItem;
import org.jboss.shamrock.opentracing.runtime.ShamrockTracingDynamicFeature;
import org.jboss.shamrock.opentracing.runtime.TracerProducer;
import org.jboss.shamrock.opentracing.runtime.TracingDeploymentTemplate;
import org.jboss.shamrock.undertow.FilterBuildItem;

import io.opentracing.contrib.interceptors.OpenTracingInterceptor;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;

public class OpentracingProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> registerBeans() {
        return Arrays.asList(new AdditionalBeanBuildItem(OpenTracingInterceptor.class, TracerProducer.class));
    }

    @BuildStep
    ReflectiveMethodBuildItem registerMethod() throws Exception {
        Method isAsync = ObserverMethod.class.getMethod("isAsync");
        return new ReflectiveMethodBuildItem(isAsync);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupFilter(BuildProducer<JaxrsProviderBuildItem> providers,
                     BuildProducer<FilterBuildItem> filterProducer,
                     TracingDeploymentTemplate tracing) {

        //TODO: this needs to be a BuildItem so that more than one can be registered
        providers.produce(new JaxrsProviderBuildItem(ShamrockTracingDynamicFeature.class.getName()));

        FilterBuildItem filterInfo = new FilterBuildItem("tracingFilter", SpanFinishingFilter.class.getName());
        filterInfo.setAsyncSupported(true);
        filterInfo.addFilterUrlMapping("*", DispatcherType.FORWARD);
        filterInfo.addFilterUrlMapping("*", DispatcherType.INCLUDE);
        filterInfo.addFilterUrlMapping("*", DispatcherType.REQUEST);
        filterInfo.addFilterUrlMapping("*", DispatcherType.ASYNC);
        filterInfo.addFilterUrlMapping("*", DispatcherType.ERROR);
        filterProducer.produce(filterInfo);

        //note that we can't put this into its own method as we need this to be registered before Undertow is started
        tracing.registerTracer();

    }

}
