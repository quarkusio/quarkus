package org.acme;

import java.security.SecureRandom;
import java.util.Random;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class SimpleService {
    private static final Logger log = Logger.getLogger(SimpleService.class);

    @Inject
    MeterRegistry registry;

    Random random = new SecureRandom();
    double[] arr = new double[1];

    @PostConstruct
    public void start() {
        String key = System.getProperty("tag-key", "test");
        Gauge.builder("xvalue", arr, a -> arr[0])
             .baseUnit("X")
             .description("Some random x")
             .tag(key, "x")
             .register(registry);
    }

    public String poke(int f) {
        log.infof("Poke %s", f);
        double x = random.nextDouble() * f;
        arr[0] = x;
        return "poke:" + x;
    }
}
