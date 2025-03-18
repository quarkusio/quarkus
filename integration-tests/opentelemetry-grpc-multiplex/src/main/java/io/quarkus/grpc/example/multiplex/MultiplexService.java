package io.quarkus.grpc.example.multiplex;

import jakarta.inject.Inject;

import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.MutinyMultiplexGrpc;
import io.grpc.examples.multiplex.StringRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;

@GrpcService
public class MultiplexService extends MutinyMultiplexGrpc.MultiplexImplBase {

    @Inject
    Tracer tracer;

    public Multi<LongReply> parse(Multi<StringRequest> request) {
        Span parentSpan = Span.current();
        return request
                .map(x -> {
                    // fixme a span per request
                    Span childSpan = tracer.spanBuilder("child")
                            .setParent(Context.current().with(parentSpan))
                            .startSpan();
                    try (Scope scope = childSpan.makeCurrent()) {
                        LongReply result = LongReply.newBuilder().setValue(Long.parseLong(x.getNumber())).build();
                        return result;
                    } finally {
                        childSpan.end();
                    }
                });
    }
}
