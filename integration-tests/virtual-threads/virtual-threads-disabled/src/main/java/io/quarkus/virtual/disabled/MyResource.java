package io.quarkus.virtual.disabled;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/")
public class MyResource {

    private final Counter counter;

    MyResource(Counter counter) {
        this.counter = counter;
    }

    @GET
    @RunOnVirtualThread
    public String testGet() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return "hello-" + counter.increment();
    }

    @POST
    @RunOnVirtualThread
    public String testPost(String body) {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return body + "-" + counter.increment();
    }

    @GET
    @NonBlocking
    @Path("/non-blocking")
    public String testNonBlocking() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return "ok";
    }

    @GET
    @Path("/uni")
    public Uni<String> testUni() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return Uni.createFrom().item("ok");
    }

    @GET
    @Path("/multi")
    public Multi<String> testMulti() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return Multi.createFrom().items("o", "k");
    }

    @GET
    @Path("/blocking")
    public String testBlocking() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return "hello-" + counter.increment();
    }

}
