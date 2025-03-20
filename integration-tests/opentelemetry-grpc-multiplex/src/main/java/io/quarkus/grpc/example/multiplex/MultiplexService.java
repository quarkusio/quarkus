package io.quarkus.grpc.example.multiplex;

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
import jakarta.inject.Inject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@GrpcService
public class MultiplexService extends MutinyMultiplexGrpc.MultiplexImplBase {


    @Inject
    Tracer tracer;

    @Inject
    Vertx vertx;

    @Inject
//    @VirtualThreads
    ExecutorService executorService;

    public Multi<LongReply> parse(Multi<StringRequest> request) {

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
                                    innerSpan.setAttribute("inner.number", x.getNumber());
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
                            childSpan.setAttribute("child.number", x.getNumber());
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
                    return LongReply.newBuilder().setValue(Long.parseLong(x.getNumber())).build();
                });
    }
}
