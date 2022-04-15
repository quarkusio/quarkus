package io.quarkus.micrometer.test;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Path("/test")
@Singleton
public class MeterResource {

    MeterRegistry registry = new SimpleMeterRegistry();

    public MeterResource() {
        Metrics.addRegistry(registry);
    }

    @GET
    @Path("requests")
    public List<String> requests() {
        return registry.find("http.server.requests").meters().stream()
                .map(x -> x.getId().getTag("uri"))
                .collect(Collectors.toList());
    }
}
