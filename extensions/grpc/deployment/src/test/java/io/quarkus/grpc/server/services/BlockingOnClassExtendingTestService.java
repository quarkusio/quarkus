package io.quarkus.grpc.server.services;

import com.google.protobuf.EmptyProtos;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

@GrpcService
@Blocking
public class BlockingOnClassExtendingTestService extends BlockingBaseTestService {
    @Override
    public void emptyCallBlocking(EmptyProtos.Empty request, StreamObserver<EmptyProtos.Empty> responseObserver) {
        super.emptyCallBlocking(request, responseObserver);
    }

    @Override
    public void unaryCallBlocking(Messages.SimpleRequest request, StreamObserver<Messages.SimpleResponse> responseObserver) {
        super.unaryCallBlocking(request, responseObserver);
    }

    @Override
    public void streamingOutputCallBlocking(Messages.StreamingOutputCallRequest request,
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        super.streamingOutputCallBlocking(request, responseObserver);
    }

    @Override
    public StreamObserver<Messages.StreamingInputCallRequest> streamingInputCallBlocking(
            StreamObserver<Messages.StreamingInputCallResponse> responseObserver) {
        return super.streamingInputCallBlocking(responseObserver);
    }

    @Override
    public StreamObserver<Messages.StreamingOutputCallRequest> fullDuplexCallBlocking(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        return super.fullDuplexCallBlocking(responseObserver);
    }

    @Override
    public StreamObserver<Messages.StreamingOutputCallRequest> halfDuplexCallBlocking(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        return super.halfDuplexCallBlocking(responseObserver);
    }
}
