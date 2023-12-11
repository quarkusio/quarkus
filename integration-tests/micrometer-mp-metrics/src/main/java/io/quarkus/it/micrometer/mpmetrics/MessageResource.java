package io.quarkus.it.micrometer.mpmetrics;

import java.util.Collection;
import java.util.Objects;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

@Path("/message")
public class MessageResource {

    private final MeterRegistry registry;
    private final Counter first;
    private final Counter second;

    public MessageResource(MeterRegistry registry,
            @Metric(name = "first-counter") final Counter first,
            @Metric(name = "second-counter") final Counter second) {
        this.registry = registry;
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    @GET
    public String message() {
        first.inc();
        second.inc();
        return registry.getClass().getName();
    }

    @GET
    @Path("fail")
    public String fail() {
        first.inc();
        throw new NullPointerException("Failed on purpose");
    }

    @GET
    @Path("item/{id}")
    public String item(@PathParam("id") String id) {
        second.inc();
        return "return message with id " + id;
    }

    @GET
    @Path("mpmetrics")
    public String metrics() {
        Collection<Meter> meters = Search.in(registry).name(s -> s.contains("mpmetrics")).meters();
        meters.addAll(Search.in(registry).name(s -> s.endsWith("-counter")).meters());
        return meters.stream().allMatch(x -> x.getId().getTag("scope") != null) ? "OK" : "FAIL";
    }
}
