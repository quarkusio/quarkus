package io.quarkus.grpc.server.services;

import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class BlockingMutinyBaseExtendingTestService
        extends BlockingMutinyBaseTestService {
    @Override
    @Blocking
    public Uni<EmptyProtos.Empty> emptyCallBlocking(EmptyProtos.Empty request) {
        return super.emptyCallBlocking(request);
    }

    @Override
    @Blocking
    public Uni<Messages.SimpleResponse> unaryCallBlocking(Messages.SimpleRequest request) {
        return super.unaryCallBlocking(request);
    }

    @Override
    @Blocking
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCallBlocking(
            Messages.StreamingOutputCallRequest request) {
        return super.streamingOutputCallBlocking(request);
    }

    @Override
    @Blocking
    public Uni<Messages.StreamingInputCallResponse> streamingInputCallBlocking(
            Multi<Messages.StreamingInputCallRequest> request) {
        return super.streamingInputCallBlocking(request);
    }

    @Override
    @Blocking
    public Multi<Messages.StreamingOutputCallResponse> fullDuplexCallBlocking(
            Multi<Messages.StreamingOutputCallRequest> request) {
        return super.fullDuplexCallBlocking(request);
    }

    @Override
    @Blocking
    public Multi<Messages.StreamingOutputCallResponse> halfDuplexCallBlocking(
            Multi<Messages.StreamingOutputCallRequest> request) {
        return super.halfDuplexCallBlocking(request);
    }
}
