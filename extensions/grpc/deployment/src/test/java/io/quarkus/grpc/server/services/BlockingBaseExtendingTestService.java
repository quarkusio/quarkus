package io.quarkus.grpc.server.services;

import com.google.protobuf.EmptyProtos;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.integration.Messages;
import io.smallrye.common.annotation.Blocking;

public class BlockingBaseExtendingTestService extends BlockingBaseTestService {
    @Override
    @Blocking
    public void emptyCallBlocking(EmptyProtos.Empty request, StreamObserver<EmptyProtos.Empty> responseObserver) {
        super.emptyCallBlocking(request, responseObserver);
    }

    @Override
    @Blocking
    public void unaryCallBlocking(Messages.SimpleRequest request, StreamObserver<Messages.SimpleResponse> responseObserver) {
        super.unaryCallBlocking(request, responseObserver);
    }

    @Override
    @Blocking
    public void streamingOutputCallBlocking(Messages.StreamingOutputCallRequest request,
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        super.streamingOutputCallBlocking(request, responseObserver);
    }

    @Override
    @Blocking
    public StreamObserver<Messages.StreamingInputCallRequest> streamingInputCallBlocking(
            StreamObserver<Messages.StreamingInputCallResponse> responseObserver) {
        return super.streamingInputCallBlocking(responseObserver);
    }

    @Override
    @Blocking
    public StreamObserver<Messages.StreamingOutputCallRequest> fullDuplexCallBlocking(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        return super.fullDuplexCallBlocking(responseObserver);
    }

    @Override
    @Blocking
    public StreamObserver<Messages.StreamingOutputCallRequest> halfDuplexCallBlocking(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        return super.halfDuplexCallBlocking(responseObserver);
    }
}
