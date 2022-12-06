package io.quarkus.it.micrometer.mpmetrics;

import jakarta.enterprise.context.RequestScoped;

import org.eclipse.microprofile.metrics.annotation.Counted;

@RequestScoped
@Counted(description = "called for each discovered prime number")
public class CountedInstance {

    CountedInstance() {
    }

    public void countPrimes() {
    }
}
