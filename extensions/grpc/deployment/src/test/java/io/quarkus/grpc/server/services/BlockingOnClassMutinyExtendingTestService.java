package io.quarkus.grpc.server.services;

import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
@Blocking
public class BlockingOnClassMutinyExtendingTestService
        extends BlockingMutinyBaseTestService {
    @Override
    public Uni<EmptyProtos.Empty> emptyCallBlocking(EmptyProtos.Empty request) {
        return super.emptyCallBlocking(request);
    }

    @Override
    public Uni<Messages.SimpleResponse> unaryCallBlocking(Messages.SimpleRequest request) {
        return super.unaryCallBlocking(request);
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCallBlocking(
            Messages.StreamingOutputCallRequest request) {
        return super.streamingOutputCallBlocking(request);
    }

    @Override
    public Uni<Messages.StreamingInputCallResponse> streamingInputCallBlocking(
            Multi<Messages.StreamingInputCallRequest> request) {
        return super.streamingInputCallBlocking(request);
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> fullDuplexCallBlocking(
            Multi<Messages.StreamingOutputCallRequest> request) {
        return super.fullDuplexCallBlocking(request);
    }

    @Override
    public Multi<Messages.StreamingOutputCallResponse> halfDuplexCallBlocking(
            Multi<Messages.StreamingOutputCallRequest> request) {
        return super.halfDuplexCallBlocking(request);
    }
}
