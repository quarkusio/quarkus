package io.quarkus.it.micrometer.mpmetrics;

import java.util.Random;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.annotation.Metric;

@Singleton
public class InjectedInstance {
    Random r = new Random();

    @Inject
    @Metric(name = "notPrime", description = "Count the number of not prime numbers")
    Counter count;

    @Inject
    @Metric(name = "valueRange", description = "Aggregate checked values")
    Histogram histogram;

    @Produces
    @Metric(name = "passiveInjection", description = "This is ignored: @Produces + @Metric")
    Gauge<Integer> createGauge = new Gauge<Integer>() {
        @Override
        public Integer getValue() {
            return r.nextInt();
        }
    };
}
