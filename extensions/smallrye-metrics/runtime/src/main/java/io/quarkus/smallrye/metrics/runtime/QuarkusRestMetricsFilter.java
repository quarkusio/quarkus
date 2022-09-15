package io.quarkus.smallrye.metrics.runtime;

import static io.quarkus.smallrye.metrics.runtime.FilterUtil.maybeCreateMetrics;

import java.lang.reflect.Method;

import jakarta.ws.rs.container.ResourceInfo;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * Quarkus REST does not suffer from the limitations mentioned in {@link QuarkusRestEasyMetricsFilter} so we can
 * properly use a response filter in order to finish the request.
 * Moreover, we use the {@code @ServerResponseFilter} to make writing the filter even easier.
 */
public class QuarkusRestMetricsFilter {

    @ServerResponseFilter
    public void filter(ResourceInfo resourceInfo, Throwable throwable) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();
        if ((resourceClass == null) || (resourceMethod == null)) {
            return;
        }
        maybeCreateMetrics(resourceClass, resourceMethod);
        FilterUtil.finishRequest(System.nanoTime(), resourceInfo.getResourceClass(),
                resourceInfo.getResourceMethod().getName(),
                resourceInfo.getResourceMethod().getParameterTypes(),
                // FIXME: we need to know whether the exception is mapped or not, how to find out?
                // for now let's assume all are unmapped, and therefore if there was an exception,
                // increment the failure counter rather than the successful calls counter
                () -> throwable == null);
    }
}
