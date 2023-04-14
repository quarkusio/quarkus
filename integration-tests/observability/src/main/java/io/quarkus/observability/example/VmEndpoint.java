package io.quarkus.observability.example;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.observability.victoriametrics.client.PushGauge;
import io.quarkus.observability.victoriametrics.client.VictoriaMetricsService;

@Path("/vm")
public class VmEndpoint {
    private static final Logger log = Logger.getLogger(VmEndpoint.class);

    @Inject
    @RestClient
    VictoriaMetricsService service;

    Random random = new SecureRandom();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/poke")
    public String poke(@QueryParam("f") int f) {
        log.infof("Poke %s", f);
        double x = random.nextDouble() * f;

        PushGauge gauge = PushGauge
                .build()
                .namespace("quarkus")
                .subsystem("observability")
                .name("xvalue")
                .help("Some random x")
                .unit("X")
                .labelNames("test")
                .create();

        gauge
                .labels("x")
                .add(x);

        VictoriaMetricsService.importPrometheus(service, Stream.of(gauge));

        return "poke:" + x;
    }
}
