package io.quarkus.virtual.disabled;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/class")
@RunOnVirtualThread
public class MyResourceWithVTOnClass {

    private final Counter counter;

    MyResourceWithVTOnClass(Counter counter) {
        this.counter = counter;
    }

    @GET
    public String testGet() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return "hello-" + counter.increment();
    }

    @POST
    public String testPost(String body) {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return body + "-" + counter.increment();
    }

    @GET
    @Path("/uni")
    @Blocking // Mandatory, because it's really a weird case
    public Uni<String> testUni() {
        return Uni.createFrom().item("ok");
    }

    @GET
    @Path("/multi")
    @Blocking // Mandatory, because it's really a weird case
    public Multi<String> testMulti() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return Multi.createFrom().items("o", "k");
    }

}
