/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.smallrye.metrics.runtime;

import java.lang.reflect.Method;
import java.time.Duration;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.metrics.MetricRegistries;
import io.vertx.ext.web.RoutingContext;

/**
 * A JAX-RS filter that computes the REST.request metrics from REST traffic over time.
 * This one depends on Vert.x to be able to hook into response even in cases when the request ended with an unmapped exception.
 */
public class QuarkusJaxRsMetricsFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        Long start = System.nanoTime();
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();
        /*
         * The reason for using a Vert.x handler instead of ContainerResponseFilter is that
         * RESTEasy does not call the response filter for requests that ended up with an unmapped exception.
         * This way we can capture these responses as well and update the metrics accordingly.
         */
        RoutingContext routingContext = CDI.current().select(CurrentVertxRequest.class).get().getCurrent();
        routingContext.addBodyEndHandler(
                event -> finishRequest(start, resourceClass, resourceMethod));
    }

    private void finishRequest(Long start, Class<?> resourceClass, Method resourceMethod) {
        long value = System.nanoTime() - start;
        MetricID metricID = getMetricID(resourceClass, resourceMethod);

        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.BASE);
        if (!registry.getMetadata().containsKey(metricID.getName())) {
            // if no metric with this name exists yet, register it
            Metadata metadata = Metadata.builder()
                    .withName(metricID.getName())
                    .withDescription(
                            "The number of invocations and total response time of this RESTful resource method since the start of the server.")
                    .withUnit(MetricUnits.NANOSECONDS)
                    .build();
            registry.simpleTimer(metadata, metricID.getTagsAsArray());
        }
        registry.simpleTimer(metricID.getName(), metricID.getTagsAsArray())
                .update(Duration.ofNanos(value));
    }

    private MetricID getMetricID(Class<?> resourceClass, Method resourceMethod) {
        Tag classTag = new Tag("class", resourceClass.getName());
        String methodName = resourceMethod.getName();
        StringBuilder sb = new StringBuilder();
        for (Class<?> parameterType : resourceMethod.getParameterTypes()) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            if (parameterType.isArray()) {
                sb.append(parameterType.getComponentType().getName()).append("[]");
            } else {
                sb.append(parameterType.getName());
            }
        }
        String encodedParameterNames = sb.toString();
        String methodTagValue = encodedParameterNames.isEmpty() ? methodName : methodName + "_" + encodedParameterNames;
        Tag methodTag = new Tag("method", methodTagValue);
        return new MetricID("REST.request", classTag, methodTag);
    }

}
