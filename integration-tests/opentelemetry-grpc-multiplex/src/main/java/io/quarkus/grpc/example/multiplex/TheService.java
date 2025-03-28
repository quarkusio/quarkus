package io.quarkus.grpc.example.multiplex;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.StringRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class TheService {
    @WithSpan
    public void processResult(StringRequest x,
            CompletableFuture<LongReply> result,
            CountDownLatch latch) {
        Span span = Span.current();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("inner.number", x.getNumber());
            result.complete(LongReply.newBuilder().setValue(Long.parseLong(x.getNumber())).build());
            latch.countDown();
        }
    }
}
