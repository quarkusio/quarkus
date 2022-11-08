package io.quarkus.rest.client.reactive;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;

import io.smallrye.mutiny.Uni;

@Path("/hello")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class HelloResource {
    @POST
    public String echo(String name, @Context Request request) {
        return "hello, " + name;
    }

    @POST
    @Path("/bytes")
    public byte[] bytes(byte[] value) {
        return value;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ints")
    public int[] bytes(int[] value) {
        return value;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/query-params-to-map")
    public Map<String, String> queryParamsToMap(@QueryParam("p1") String p1, @QueryParam("p2(") String p2,
            @QueryParam("p3[") String p3, @QueryParam("p4?") String p4, @QueryParam("p5=") String p5,
            @QueryParam("p6-") String p6) {
        Map<String, String> map = new HashMap<>();
        map.put("p1", p1);
        map.put("p2", p2);
        map.put("p3", p3);
        map.put("p4", p4);
        map.put("p5", p5);
        map.put("p6", p6);
        return map;
    }

    @RestClient
    HelloClient2 client2;

    @GET
    public Uni<String> something() {
        Thread thread = Thread.currentThread();
        return client2.delay()
                .map(foo -> {
                    Assertions.assertSame(thread, Thread.currentThread());
                    return foo;
                });
    }

    @Path("delay")
    @GET
    public Uni<String> delay() {
        return Uni.createFrom().item("Hello")
                .onItem().delayIt().by(Duration.ofMillis(500));
    }

    @Path("callClientForImageInfo")
    @GET
    public String callClientForImageInfo() {
        int size = 1024 * 1024 * 5;

        byte[] buffer = new byte[size];

        //Should provoke 415 Unsupported Media Type
        return client2.imageInfo(buffer);
    }

    @POST
    @Consumes({ "image/jpeg", "image/png" })
    @Path("/imageInfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String imageInfo(byte[] imageFile) {
        throw new IllegalStateException("This method should never be invoked");
    }
}
