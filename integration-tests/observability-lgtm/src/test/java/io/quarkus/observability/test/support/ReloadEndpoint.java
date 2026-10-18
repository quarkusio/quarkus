package io.quarkus.observability.test.support;

import java.security.SecureRandom;
import java.util.Random;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * This is a copy of SimpleEndpoint.
 * Since it must be in test classes to be added as source.
 */
@Path("/reload")
public class ReloadEndpoint {
    private static final Logger log = Logger.getLogger(ReloadEndpoint.class);

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

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/poke")
    public String poke(@QueryParam("f") int f) {
        log.infof("Poke %s", f);
        double x = random.nextDouble() * f;
        arr[0] = x;
        return "poke:" + x;
    }
}
