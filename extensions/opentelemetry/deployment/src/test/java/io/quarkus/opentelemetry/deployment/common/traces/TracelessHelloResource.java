package io.quarkus.opentelemetry.deployment.common.traces;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.opentelemetry.api.metrics.Meter;
import io.quarkus.opentelemetry.runtime.tracing.Traceless;

@Path("/hello")
public class TracelessHelloResource {

    @Inject
    Meter meter;

    @GET
    @Traceless
    public String hello() {
        meter.counterBuilder("hello").build().add(1);
        return "hello";
    }

    @Path("/hi")
    @GET
    @Traceless
    public String hi() {
        meter.counterBuilder("hi").build().add(1);
        return "hi";
    }

    @Path("no-slash")
    @GET
    @Traceless
    public String noSlash() {
        meter.counterBuilder("no-slash").build().add(1);
        return "no-slash";
    }

    @GET
    @Path("/mask/{number}")
    @Traceless
    public String mask(@PathParam("number") String number) {
        meter.counterBuilder("mask").build().add(Integer.parseInt(number));
        return "mask-" + number;
    }
}
