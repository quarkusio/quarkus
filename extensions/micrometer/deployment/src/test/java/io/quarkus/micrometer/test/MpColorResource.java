package io.quarkus.micrometer.test;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;

@ApplicationScoped
public class MpColorResource {
    @Counted
    public void red() {
        // ...
    }

    @Counted(name = "blueCount")
    public void blue() {
        // ...
    }

    @Counted(name = "greenCount", absolute = true)
    public void green() {
        // ...
    }

    @Gauge(absolute = true, unit = "jellybeans")
    public long yellow() {
        return 0L;
    }
}
