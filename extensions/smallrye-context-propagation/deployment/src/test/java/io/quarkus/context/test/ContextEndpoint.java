package io.quarkus.context.test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.Assertions;
import org.wildfly.common.Assert;

import io.quarkus.arc.Arc;

@Path("/context")
public class ContextEndpoint {

    @Inject
    RequestBean doNotRemoveMe;

    @GET
    @Path("/resteasy")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<String> resteasyTest(@Context UriInfo uriInfo) {
        ManagedExecutor me = ManagedExecutor.builder().build();
        CompletableFuture<String> ret = me.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            URI uri = uriInfo.getAbsolutePath();
            System.err.println("Got URI: " + uri);
            return text;
        });
    }

    @GET
    @Path("/arc")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<String> arcTest() {
        ManagedExecutor me = ManagedExecutor.builder().build();
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        System.err.println("Got bean1: " + previousValue);
        CompletableFuture<String> ret = me.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
            System.err.println("Got bean2: " + instance2.callMe());
            Assertions.assertEquals(previousValue, instance2.callMe());
            return text;
        });
    }

    @GET
    @Path("/noarc")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<String> noarcTest() {
        ManagedExecutor me = ManagedExecutor.builder().cleared("CDI").build();
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        System.err.println("Got bean1: " + previousValue);
        CompletableFuture<String> ret = me.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
            System.err.println("Got bean2: " + instance2.callMe());
            Assertions.assertNotEquals(previousValue, instance2.callMe());
            return text;
        });
    }
}
