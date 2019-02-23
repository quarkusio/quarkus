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

package io.quarkus.smallrye.opentracing;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.servlet.DispatcherType;

import io.opentracing.contrib.interceptors.OpenTracingInterceptor;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import io.quarkus.resteasy.ResteasyJaxrsProviderBuildItem;
import io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingDynamicFeature;
import io.quarkus.smallrye.opentracing.runtime.TracerProducer;
import io.quarkus.undertow.FilterBuildItem;

public class SmallRyeOpenTracingProcessor {

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
    void setupFilter(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<FilterBuildItem> filterProducer,
            BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_OPENTRACING));

        providers.produce(new ResteasyJaxrsProviderBuildItem(QuarkusSmallRyeTracingDynamicFeature.class.getName()));

        FilterBuildItem filterInfo = new FilterBuildItem("tracingFilter", SpanFinishingFilter.class.getName());
        filterInfo.setAsyncSupported(true);
        filterInfo.addFilterUrlMapping("*", DispatcherType.FORWARD);
        filterInfo.addFilterUrlMapping("*", DispatcherType.INCLUDE);
        filterInfo.addFilterUrlMapping("*", DispatcherType.REQUEST);
        filterInfo.addFilterUrlMapping("*", DispatcherType.ASYNC);
        filterInfo.addFilterUrlMapping("*", DispatcherType.ERROR);
        filterProducer.produce(filterInfo);
    }

}
