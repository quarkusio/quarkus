package io.quarkus.smallrye.metrics.runtime;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimplifiedResourceInfo;

/**
 * Quarkus REST does not suffer from the limitations mentioned in {@link QuarkusRestEasyMetricsFilter} so we can
 * properly use a response filter in order to finish the request.
 * Moreover we use the {@code @ServerResponseFilter} to make writing the filter even easier.
 */
public class QuarkusRestMetricsFilter {

    @ServerResponseFilter
    public void filter(SimplifiedResourceInfo simplifiedResourceInfo) {
        FilterUtil.finishRequest(System.nanoTime(), simplifiedResourceInfo.getResourceClass(),
                simplifiedResourceInfo.getMethodName(),
                simplifiedResourceInfo.parameterTypes());
    }
}
