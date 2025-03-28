package io.quarkus.grpc.example.multiplex;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.MutinyMultiplexGrpc;
import io.grpc.examples.multiplex.StringRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;

@GrpcService
public class MultiplexResource extends MutinyMultiplexGrpc.MultiplexImplBase {

    @Inject
    Tracer tracer;

    @Inject
    Vertx vertx;

    @Inject
    TheService service;

    @Inject
    //    @VirtualThreads
    ExecutorService executorService;

    public Multi<LongReply> parse(Multi<StringRequest> request) {

        Span parentSpan = Span.current();

        return request
                .emitOn(executorService)
                .map(x -> {
                    final CompletableFuture<LongReply> result = new CompletableFuture<>();
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

                                service.processResult(x, result, latch);
                            });

                            try {
                                latch.await(2, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            childSpan.setAttribute("child.number", x.getNumber());
                            outerLatch.countDown();

                        } finally {
                            childSpan.end();
                        }

                    });

                    try {
                        outerLatch.await(2, TimeUnit.SECONDS);
                        return result.get(2, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
