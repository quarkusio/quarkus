package io.quarkus.grpc.server.services;

import static io.quarkus.grpc.server.services.AssertHelper.assertRunOnEventLoop;
import static io.quarkus.grpc.server.services.AssertHelper.assertRunOnWorker;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.blocking.MutinyBlockingTestServiceGrpc;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class BlockingMutinyTestService
        extends MutinyBlockingTestServiceGrpc.BlockingTestServiceImplBase {

    @Override
    public Uni<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
        assertThat(request).isNotNull();
        assertRunOnEventLoop();
        return Uni.createFrom().item(EmptyProtos.Empty.newBuilder().build());
    }

    @Override
    @Blocking
    public Uni<EmptyProtos.Empty> emptyCallBlocking(EmptyProtos.Empty request) {
        assertThat(request).isNotNull();
        assertRunOnWorker();
        return Uni.createFrom().item(EmptyProtos.Empty.newBuilder().build());
    }

    @Override
    public Uni<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
        assertThat(request).isNotNull();
        assertRunOnEventLoop();
        return Uni.createFrom().item(Messages.SimpleResponse.newBuilder().build());
    }

    @Override
    @Blocking
    public Uni<Messages.SimpleResponse> unaryCallBlocking(Messages.SimpleRequest request) {
        assertThat(request).isNotNull();
        assertRunOnWorker();
        return Uni.createFrom().item(Messages.SimpleResponse.newBuilder().build());
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCall(
            Messages.StreamingOutputCallRequest request) {
        assertThat(request).isNotNull();
        assertRunOnEventLoop();
        return Multi.createFrom().range(0, 10)
                .map(i -> ByteString.copyFromUtf8(Integer.toString(i)))
                .map(s -> Messages.Payload.newBuilder().setBody(s).build())
                .map(p -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(p).build());
    }

    @Override
    @Blocking
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCallBlocking(
            Messages.StreamingOutputCallRequest request) {
        assertThat(request).isNotNull();
        assertRunOnWorker();
        return Multi.createFrom().range(0, 10)
                .map(i -> ByteString.copyFromUtf8(Integer.toString(i)))
                .map(s -> Messages.Payload.newBuilder().setBody(s).build())
                .map(p -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(p).build());
    }

    @Override
    public Uni<Messages.StreamingInputCallResponse> streamingInputCall(
            Multi<Messages.StreamingInputCallRequest> request) {
        assertRunOnEventLoop();
        return request.map(i -> i.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .map(list -> {
                    assertRunOnEventLoop();
                    assertThat(list).containsExactly("a", "b", "c", "d");
                    return Messages.StreamingInputCallResponse.newBuilder().build();
                });
    }

    @Override
    @Blocking
    public Uni<Messages.StreamingInputCallResponse> streamingInputCallBlocking(
            Multi<Messages.StreamingInputCallRequest> request) {
        assertRunOnWorker();
        return request.map(i -> i.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .map(list -> {
                    assertRunOnWorker();
                    assertThat(list).containsExactly("a", "b", "c", "d");
                    return Messages.StreamingInputCallResponse.newBuilder().build();
                });
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> fullDuplexCall(
            Multi<Messages.StreamingOutputCallRequest> request) {
        AtomicInteger counter = new AtomicInteger();
        assertRunOnEventLoop();
        return request
                .map(r -> r.getPayload().getBody().toStringUtf8())
                .map(r -> {
                    assertRunOnEventLoop();
                    return r + counter.incrementAndGet();
                })
                .map(r -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(r)).build())
                .map(r -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(r).build());
    }

    @Override
    @Blocking
    public Multi<Messages.StreamingOutputCallResponse> fullDuplexCallBlocking(
            Multi<Messages.StreamingOutputCallRequest> request) {
        AtomicInteger counter = new AtomicInteger();
        assertRunOnWorker();
        return request
                .map(r -> r.getPayload().getBody().toStringUtf8())
                .map(r -> {
                    assertRunOnWorker();
                    return r + counter.incrementAndGet();
                })
                .map(r -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(r)).build())
                .map(r -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(r).build());
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> halfDuplexCall(
            Multi<Messages.StreamingOutputCallRequest> request) {
        assertRunOnEventLoop();
        return request
                .map(r -> {
                    assertRunOnEventLoop();
                    return r.getPayload().getBody().toStringUtf8();
                })
                .map(String::toUpperCase)
                .collect().asList()
                .onItem().transformToMulti(s -> Multi.createFrom().iterable(s))
                .map(r -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(r)).build())
                .map(r -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(r).build());

    }

    @Override
    @Blocking
    public Multi<Messages.StreamingOutputCallResponse> halfDuplexCallBlocking(
            Multi<Messages.StreamingOutputCallRequest> request) {
        assertRunOnWorker();
        return request
                .map(r -> {
                    assertRunOnWorker();
                    return r.getPayload().getBody().toStringUtf8();
                })
                .map(String::toUpperCase)
                .collect().asList()
                .onItem().transformToMulti(s -> Multi.createFrom().iterable(s))
                .map(r -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(r)).build())
                .map(r -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(r).build());

    }
}
