package io.quarkus.resteasy.test.asyncio;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Path("asyncio")
public class AsyncIOResource {

    @Inject
    Vertx vertx;

    @GET
    public CompletionStage<String> getOnIOThread() {
        CompletableFuture<String> ret = new CompletableFuture<>();
        vertx.setTimer(100, res -> {
            ret.complete(Context.isOnEventLoopThread() ? "OK" : "not on event loop");
        });
        return ret;
    }
}
