package io.quarkus.grpc.example.streaming;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TestServiceImpl implements TestService {

    @RunOnVirtualThread
    @Override
    public Uni<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
        VirtualThreadsAssertions.assertEverything();
        return Uni.createFrom().item(EmptyProtos.Empty.newBuilder().build())
                .invoke(VirtualThreadsAssertions::assertEverything);
    }

    @RunOnVirtualThread
    @Override
    public Uni<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
        VirtualThreadsAssertions.assertEverything();
        var value = request.getPayload().getBody().toStringUtf8();
        var resp = Messages.SimpleResponse.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(value.toUpperCase())).build())
                .build();
        return Uni.createFrom().item(resp)
                .invoke(VirtualThreadsAssertions::assertEverything);
    }

    @Override
    @RunOnVirtualThread
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCall(Messages.StreamingOutputCallRequest request) {
        var value = request.getPayload().getBody().toStringUtf8();
        VirtualThreadsAssertions.assertEverything();
        return Multi.createFrom().<String> emitter(emitter -> {
            VirtualThreadsAssertions.assertEverything();
            emitter.emit(value.toUpperCase());
            emitter.emit(value.toUpperCase());
            emitter.emit(value.toUpperCase());
            emitter.complete();
        }).map(v -> Messages.StreamingOutputCallResponse.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(v)).build())
                .build())
                .invoke(VirtualThreadsAssertions::assertEverything)
                .onTermination().invoke(VirtualThreadsAssertions::assertEverything);
    }

}
