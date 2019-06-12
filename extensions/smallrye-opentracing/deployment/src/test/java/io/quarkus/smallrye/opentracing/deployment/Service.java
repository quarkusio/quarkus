package io.quarkus.smallrye.opentracing.deployment;

import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.opentracing.Traced;

import io.opentracing.Tracer;

@Traced
@ApplicationScoped
public class Service {

    @Inject
    Tracer tracer;

    public void foo() {
    }

    @Fallback(fallbackMethod = "fallback")
    @Timeout(value = 20L, unit = ChronoUnit.MILLIS)
    @Retry(delay = 10L, maxRetries = 2)
    public String faultTolerance() {
        tracer.buildSpan("ft").start().finish();
        throw new RuntimeException();
    }

    public String fallback() {
        return "fallback";
    }
}
