package io.quarkus.grpc.server.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.MutinyTestServiceGrpc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Singleton
public class MutinyTestService extends MutinyTestServiceGrpc.TestServiceImplBase {

    @Override
    public Uni<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
        assertThat(request).isNotNull();
        return Uni.createFrom().item(EmptyProtos.Empty.newBuilder().build());
    }

    @Override
    public Uni<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
        assertThat(request).isNotNull();
        return Uni.createFrom().item(Messages.SimpleResponse.newBuilder().build());
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCall(
            Messages.StreamingOutputCallRequest request) {
        assertThat(request).isNotNull();
        return Multi.createFrom().range(0, 10)
                .map(i -> ByteString.copyFromUtf8(Integer.toString(i)))
                .map(s -> Messages.Payload.newBuilder().setBody(s).build())
                .map(p -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(p).build());
    }

    @Override
    public Uni<Messages.StreamingInputCallResponse> streamingInputCall(
            Multi<Messages.StreamingInputCallRequest> request) {
        return request.map(i -> i.getPayload().getBody().toStringUtf8())
                .collectItems().asList()
                .map(list -> {
                    assertThat(list).containsExactly("a", "b", "c", "d");
                    return Messages.StreamingInputCallResponse.newBuilder().build();
                });
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> fullDuplexCall(
            Multi<Messages.StreamingOutputCallRequest> request) {
        AtomicInteger counter = new AtomicInteger();
        return request
                .map(r -> r.getPayload().getBody().toStringUtf8())
                .map(r -> r + counter.incrementAndGet())
                .map(r -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(r)).build())
                .map(r -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(r).build());
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> halfDuplexCall(
            Multi<Messages.StreamingOutputCallRequest> request) {
        return request
                .map(r -> r.getPayload().getBody().toStringUtf8())
                .map(String::toUpperCase)
                .collectItems().asList()
                .onItem().transformToMulti(s -> Multi.createFrom().iterable(s))
                .map(r -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(r)).build())
                .map(r -> Messages.StreamingOutputCallResponse.newBuilder().setPayload(r).build());

    }
}
