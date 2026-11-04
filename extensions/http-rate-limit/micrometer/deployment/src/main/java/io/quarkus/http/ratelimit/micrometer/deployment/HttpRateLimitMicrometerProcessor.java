package io.quarkus.http.ratelimit.micrometer.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.http.ratelimit.micrometer.runtime.internal.HttpRateLimitMetrics;

public class HttpRateLimitMicrometerProcessor {

    @BuildStep
    void metrics(HttpRateLimitMicrometerBuildTimeConfig config, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (!config.metricsEnabled()) {
            return;
        }
        beans.produce(AdditionalBeanBuildItem.builder().addBeanClass(HttpRateLimitMetrics.class).build());
    }
}
