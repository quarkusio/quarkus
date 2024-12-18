package io.quarkus.micrometer.opentelemetry.services;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.aop.MeterTag;

@ApplicationScoped
public class CountedBean {
    @Counted(value = "metric.none", recordFailuresOnly = true)
    public void onlyCountFailures() {
    }

    @Counted(value = "metric.all", extraTags = { "extra", "tag" })
    public void countAllInvocations(@MeterTag(key = "do_fail", resolver = TestValueResolver.class) boolean fail) {
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }
    }

    @Counted(description = "nice description")
    public void emptyMetricName(@MeterTag boolean fail) {
        if (fail) {
            throw new NullPointerException("Failed on purpose");
        }
    }
}
