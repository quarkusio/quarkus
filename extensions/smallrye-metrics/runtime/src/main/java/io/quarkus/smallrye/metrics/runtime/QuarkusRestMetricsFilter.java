package io.quarkus.smallrye.metrics.runtime;

import org.jboss.resteasy.reactive.ContainerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.SimplifiedResourceInfo;

/**
 * Quarkus REST does not suffer from the limitations mentioned in {@link QuarkusRestEasyMetricsFilter} so we can
 * properly use a response filter in order to finish the request.
 * Moreover we use the {@code @ContainerResponseFilter} to make writing the filter even easier.
 */
public class QuarkusRestMetricsFilter {

    @ContainerResponseFilter
    public void filter(SimplifiedResourceInfo simplifiedResourceInfo) {
        FilterUtil.finishRequest(System.nanoTime(), simplifiedResourceInfo.getResourceClass(),
                simplifiedResourceInfo.getMethodName(),
                simplifiedResourceInfo.parameterTypes());
    }
}
