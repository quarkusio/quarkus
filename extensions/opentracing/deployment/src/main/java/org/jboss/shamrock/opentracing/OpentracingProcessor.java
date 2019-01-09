/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
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
                     TracingDeploymentTemplate tracing,
                     BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.MP_OPENTRACING));
        
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
