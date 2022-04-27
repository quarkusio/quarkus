package io.quarkus.it.micrometer.prometheus;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

@Path("/message")
public class MessageResource {

    private final MeterRegistry registry;

    private final Random random = new Random();
    private LongAdder count = new LongAdder();
    private DoubleAdder duration = new DoubleAdder();

    public MessageResource(MeterRegistry registry) {
        this.registry = registry;
        registry.more().timer("function.timer", Tags.empty(), this, c -> c.getCount(), c -> c.getDuration(), TimeUnit.SECONDS);
    }

    @GET
    public String message() {
        bump();
        return registry.getClass().getName();
    }

    @GET
    @Path("fail")
    public String fail() {
        bump();
        throw new NullPointerException("Failed on purpose");
    }

    @GET
    @Path("item/{id}")
    public String item(@PathParam("id") String id) {
        bump();
        return "return message with id " + id;
    }

    @GET
    @Path("match/{id}/{sub}")
    public String match(@PathParam("id") String id, @PathParam("sub") String sub) {
        bump();
        return "return message with id " + id + ", and sub " + sub;
    }

    @GET
    @Path("match/{text}")
    public String optional(@PathParam("text") String text) {
        bump();
        return "return message with text " + text;
    }

    void bump() {
        count.increment();
        duration.add(random.nextDouble());
    }

    long getCount() {
        return count.longValue();
    }

    double getDuration() {
        return duration.doubleValue();
    }
}
