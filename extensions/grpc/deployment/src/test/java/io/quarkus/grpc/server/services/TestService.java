package io.quarkus.grpc.server.services;

import static io.quarkus.grpc.server.services.AssertHelper.assertThatTheRequestScopeIsActive;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestServiceGrpc;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class TestService extends TestServiceGrpc.TestServiceImplBase {

    @Override
    public void emptyCall(EmptyProtos.Empty request, StreamObserver<EmptyProtos.Empty> responseObserver) {
        assertThatTheRequestScopeIsActive();
        assertThat(request).isNotNull();
        responseObserver.onNext(EmptyProtos.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unaryCall(Messages.SimpleRequest request,
            StreamObserver<Messages.SimpleResponse> responseObserver) {
        assertThatTheRequestScopeIsActive();
        assertThat(request).isNotNull();
        responseObserver.onNext(Messages.SimpleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void streamingOutputCall(Messages.StreamingOutputCallRequest request,
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertThatTheRequestScopeIsActive();
        assertThat(request).isNotNull();
        for (int i = 0; i < 10; i++) {
            ByteString value = ByteString.copyFromUtf8(Integer.toString(i));
            Messages.Payload payload = Messages.Payload.newBuilder().setBody(value).build();
            responseObserver.onNext(Messages.StreamingOutputCallResponse.newBuilder().setPayload(payload).build());
        }
        // Send the completion signal on another thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                responseObserver.onCompleted();
            }
        }).start();

    }

    @Override
    public StreamObserver<Messages.StreamingInputCallRequest> streamingInputCall(
            StreamObserver<Messages.StreamingInputCallResponse> responseObserver) {
        assertThatTheRequestScopeIsActive();
        List<String> list = new CopyOnWriteArrayList<>();
        return new StreamObserver<Messages.StreamingInputCallRequest>() {
            @Override
            public void onNext(Messages.StreamingInputCallRequest streamingInputCallRequest) {
                list.add(streamingInputCallRequest.getPayload().getBody().toStringUtf8());
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                assertThat(list).containsExactly("a", "b", "c", "d");
                responseObserver.onNext(Messages.StreamingInputCallResponse.newBuilder().build());
                assertThatTheRequestScopeIsActive();
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Messages.StreamingOutputCallRequest> fullDuplexCall(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertThatTheRequestScopeIsActive();
        AtomicInteger counter = new AtomicInteger();
        return new StreamObserver<Messages.StreamingOutputCallRequest>() {
            @Override
            public void onNext(Messages.StreamingOutputCallRequest streamingOutputCallRequest) {
                Messages.Payload payload = streamingOutputCallRequest.getPayload();
                ByteString value = ByteString
                        .copyFromUtf8(payload.getBody().toStringUtf8() + counter.incrementAndGet());
                Messages.Payload resp = Messages.Payload.newBuilder().setBody(value).build();
                Messages.StreamingOutputCallResponse response = Messages.StreamingOutputCallResponse.newBuilder()
                        .setPayload(resp).build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                assertThatTheRequestScopeIsActive();
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Messages.StreamingOutputCallRequest> halfDuplexCall(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertThatTheRequestScopeIsActive();
        List<Messages.StreamingOutputCallResponse> list = new CopyOnWriteArrayList<>();
        return new StreamObserver<Messages.StreamingOutputCallRequest>() {
            @Override
            public void onNext(Messages.StreamingOutputCallRequest streamingOutputCallRequest) {
                assertThatTheRequestScopeIsActive();
                String payload = streamingOutputCallRequest.getPayload().getBody().toStringUtf8();
                ByteString value = ByteString.copyFromUtf8(payload.toUpperCase());
                Messages.Payload response = Messages.Payload.newBuilder().setBody(value).build();
                Messages.StreamingOutputCallResponse resp = Messages.StreamingOutputCallResponse.newBuilder()
                        .setPayload(response).build();
                list.add(resp);
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                assertThatTheRequestScopeIsActive();
                list.forEach(responseObserver::onNext);
                responseObserver.onCompleted();
            }
        };
    }
}
