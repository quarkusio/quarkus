package io.quarkus.virtual.rr;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

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
        AssertHelper.assertEverything();
        return "hello-" + counter.increment();
    }

    @POST
    @RunOnVirtualThread
    public String testPost(String body) {
        AssertHelper.assertEverything();
        return body + "-" + counter.increment();
    }

    @GET
    @NonBlocking
    @Path("/non-blocking")
    public String testNonBlocking() {
        AssertHelper.assertThatTheRequestScopeIsActive();
        AssertHelper.assertThatItRunsOnADuplicatedContext();
        AssertHelper.assertNotOnVirtualThread();
        return "ok";
    }

    @GET
    @Path("/uni")
    public Uni<String> testUni() {
        AssertHelper.assertThatTheRequestScopeIsActive();
        AssertHelper.assertThatItRunsOnADuplicatedContext();
        AssertHelper.assertNotOnVirtualThread();
        return Uni.createFrom().item("ok");
    }

    @GET
    @Path("/multi")
    public Multi<String> testMulti() {
        AssertHelper.assertThatTheRequestScopeIsActive();
        AssertHelper.assertThatItRunsOnADuplicatedContext();
        AssertHelper.assertNotOnVirtualThread();
        return Multi.createFrom().items("o", "k");
    }

    @GET
    @Path("/blocking")
    public String testBlocking() {
        AssertHelper.assertThatTheRequestScopeIsActive();
        AssertHelper.assertThatItRunsOnADuplicatedContext();
        AssertHelper.assertNotOnVirtualThread();
        return "hello-" + counter.increment();
    }

}
