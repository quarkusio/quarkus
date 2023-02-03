/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.smallrye.metrics.runtime;

import static io.quarkus.smallrye.metrics.runtime.FilterUtil.maybeCreateMetrics;

import java.lang.reflect.Method;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * A JAX-RS filter that computes the REST.request metrics from REST traffic over time.
 * This one depends on Vert.x to be able to hook into response even in cases when the request ended with an unmapped exception.
 */
public class QuarkusRestEasyMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        Long start = System.nanoTime();
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        final Method resourceMethod = resourceInfo.getResourceMethod();
        maybeCreateMetrics(resourceClass, resourceMethod);

        /*
         * The reason for using a Vert.x handler instead of ContainerResponseFilter is that
         * RESTEasy does not call the response filter for requests that ended up with an unmapped exception.
         * This way we can capture these responses as well and update the metrics accordingly.
         *
         */
        RoutingContext routingContext = CDI.current().select(CurrentVertxRequest.class).get().getCurrent();
        routingContext.addBodyEndHandler(
                event -> FilterUtil.finishRequest(start, resourceClass, resourceMethod.getName(),
                        resourceMethod.getParameterTypes(),
                        () -> requestContext.getProperty("smallrye.metrics.jaxrs.successful") != null));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // mark the request as successful if it finished without exception or with a mapped exception
        // if it ended with an unmapped exception, this filter is not called by RESTEasy
        requestContext.setProperty("smallrye.metrics.jaxrs.successful", true);
    }

}
