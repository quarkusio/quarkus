package io.quarkus.grpc.example.streaming;

import static io.quarkus.grpc.example.streaming.AssertHelper.assertEverything;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TestServiceImpl implements TestService {

    @RunOnVirtualThread
    @Override
    public Uni<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
        assertEverything();
        return Uni.createFrom().item(EmptyProtos.Empty.newBuilder().build())
                .invoke(AssertHelper::assertEverything);
    }

    @RunOnVirtualThread
    @Override
    public Uni<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
        assertEverything();
        var value = request.getPayload().getBody().toStringUtf8();
        var resp = Messages.SimpleResponse.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(value.toUpperCase())).build())
                .build();
        return Uni.createFrom().item(resp)
                .invoke(AssertHelper::assertEverything);
    }

    @Override
    @RunOnVirtualThread
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCall(Messages.StreamingOutputCallRequest request) {
        var value = request.getPayload().getBody().toStringUtf8();
        assertEverything();
        return Multi.createFrom().<String> emitter(emitter -> {
            assertEverything();
            emitter.emit(value.toUpperCase());
            emitter.emit(value.toUpperCase());
            emitter.emit(value.toUpperCase());
            emitter.complete();
        }).map(v -> Messages.StreamingOutputCallResponse.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(v)).build())
                .build())
                .invoke(AssertHelper::assertEverything)
                .onTermination().invoke(AssertHelper::assertEverything);
    }

}
