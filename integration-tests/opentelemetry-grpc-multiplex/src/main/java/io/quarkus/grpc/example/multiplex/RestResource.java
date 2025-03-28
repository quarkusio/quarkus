package io.quarkus.grpc.example.multiplex;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;

@Path("hello")
@Produces(MediaType.APPLICATION_JSON)
public class RestResource {

    @Inject
    Tracer tracer;

    @Inject
    Vertx vertx;

    @Inject
    //    @VirtualThreads
    ExecutorService executorService;

    @GET
    public Multi<String> parse(Multi<String> request) {

        Span parentSpan = Span.current();

        return request
                .emitOn(executorService)
                .map(x -> {

                    final io.vertx.core.Context newerContext = VertxContext.createNewDuplicatedContext();
                    CountDownLatch outerLatch = new CountDownLatch(1);
                    newerContext.runOnContext(childEvent -> {
                        Span childSpan = tracer.spanBuilder("child")
                                .setParent(Context.current().with(parentSpan))
                                .startSpan();

                        try (Scope outerScope = childSpan.makeCurrent()) {
                            final io.vertx.core.Context innerVertxContext = vertx.getOrCreateContext();
                            CountDownLatch latch = new CountDownLatch(1);
                            VertxContext.createNewDuplicatedContext(innerVertxContext).runOnContext(event -> {
                                Span innerSpan = tracer.spanBuilder("inner")
                                        .setParent(Context.current().with(childSpan))
                                        .startSpan();

                                try (Scope scope = innerSpan.makeCurrent()) {
                                    innerSpan.setAttribute("inner.number", x);
                                    //                                    LongReply result = LongReply.newBuilder().setValue(Long.parseLong(x.getNumber())).build();
                                    latch.countDown();
                                } finally {
                                    innerSpan.end();
                                }
                            });

                            try {
                                latch.await(2, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            childSpan.setAttribute("child.number", x);
                            outerLatch.countDown();

                        } finally {
                            childSpan.end();
                        }

                    });

                    try {
                        outerLatch.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return x;
                });
    }
}
