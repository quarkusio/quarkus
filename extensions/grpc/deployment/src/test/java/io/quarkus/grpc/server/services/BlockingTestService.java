package io.quarkus.grpc.server.services;

import static io.quarkus.grpc.server.services.AssertHelper.assertRunOnDuplicatedContext;
import static io.quarkus.grpc.server.services.AssertHelper.assertRunOnEventLoop;
import static io.quarkus.grpc.server.services.AssertHelper.assertRunOnWorker;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.blocking.BlockingTestServiceGrpc;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@GrpcService
public class BlockingTestService extends BlockingTestServiceGrpc.BlockingTestServiceImplBase {

    @Override
    public void emptyCall(EmptyProtos.Empty request, StreamObserver<EmptyProtos.Empty> responseObserver) {
        assertThat(request).isNotNull();
        assertRunOnEventLoop();
        assertRunOnDuplicatedContext();
        responseObserver.onNext(EmptyProtos.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void emptyCallBlocking(EmptyProtos.Empty request, StreamObserver<EmptyProtos.Empty> responseObserver) {
        assertThat(request).isNotNull();
        assertRunOnWorker();
        assertRunOnDuplicatedContext();
        responseObserver.onNext(EmptyProtos.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unaryCall(Messages.SimpleRequest request,
            StreamObserver<Messages.SimpleResponse> responseObserver) {
        assertThat(request).isNotNull();
        assertRunOnEventLoop();
        assertRunOnDuplicatedContext();
        responseObserver.onNext(Messages.SimpleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void unaryCallBlocking(Messages.SimpleRequest request,
            StreamObserver<Messages.SimpleResponse> responseObserver) {
        assertThat(request).isNotNull();
        assertRunOnWorker();
        assertRunOnDuplicatedContext();
        String msg = request.getMsg();
        if ("IllegalArgument".equals(msg)) {
            throw new IllegalArgumentException("You're not allowed!");
        }
        responseObserver.onNext(Messages.SimpleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void streamingOutputCall(Messages.StreamingOutputCallRequest request,
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertThat(request).isNotNull();
        assertRunOnEventLoop();
        assertRunOnDuplicatedContext();
        for (int i = 0; i < 10; i++) {
            ByteString value = ByteString.copyFromUtf8(i + "-" + Thread.currentThread().getName());
            Messages.Payload payload = Messages.Payload.newBuilder().setBody(value).build();
            responseObserver.onNext(Messages.StreamingOutputCallResponse.newBuilder().setPayload(payload).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void streamingOutputCallBlocking(Messages.StreamingOutputCallRequest request,
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertThat(request).isNotNull();
        assertRunOnWorker();
        assertRunOnDuplicatedContext();
        for (int i = 0; i < 10; i++) {
            ByteString value = ByteString.copyFromUtf8(i + "-" + Thread.currentThread().getName());
            Messages.Payload payload = Messages.Payload.newBuilder().setBody(value).build();
            responseObserver.onNext(Messages.StreamingOutputCallResponse.newBuilder().setPayload(payload).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Messages.StreamingInputCallRequest> streamingInputCall(
            StreamObserver<Messages.StreamingInputCallResponse> responseObserver) {
        List<String> list = new CopyOnWriteArrayList<>();
        assertRunOnEventLoop();
        Context ctxt = assertRunOnDuplicatedContext();
        return new StreamObserver<>() {
            @Override
            public void onNext(Messages.StreamingInputCallRequest streamingInputCallRequest) {
                assertRunOnEventLoop();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                list.add(streamingInputCallRequest.getPayload().getBody().toStringUtf8());
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                assertThat(list).containsExactly("a", "b", "c", "d");
                assertRunOnEventLoop();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                responseObserver.onNext(Messages.StreamingInputCallResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    @Blocking
    public StreamObserver<Messages.StreamingInputCallRequest> streamingInputCallBlocking(
            StreamObserver<Messages.StreamingInputCallResponse> responseObserver) {
        List<String> list = new CopyOnWriteArrayList<>();
        assertRunOnWorker();
        Context ctxt = assertRunOnDuplicatedContext();
        return new StreamObserver<Messages.StreamingInputCallRequest>() {
            @Override
            public void onNext(Messages.StreamingInputCallRequest streamingInputCallRequest) {
                assertRunOnWorker();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                list.add(streamingInputCallRequest.getPayload().getBody().toStringUtf8());
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                assertThat(list).containsExactly("a", "b", "c", "d");
                assertRunOnWorker();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                responseObserver.onNext(Messages.StreamingInputCallResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Messages.StreamingOutputCallRequest> fullDuplexCall(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        AtomicInteger counter = new AtomicInteger();
        assertRunOnEventLoop();
        Context ctxt = assertRunOnDuplicatedContext();
        return new StreamObserver<>() {
            @Override
            public void onNext(Messages.StreamingOutputCallRequest streamingOutputCallRequest) {
                assertRunOnEventLoop();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                Messages.Payload payload = streamingOutputCallRequest.getPayload();
                ByteString value = ByteString
                        .copyFromUtf8(payload.getBody().toStringUtf8() + counter.incrementAndGet() + "-"
                                + Thread.currentThread().getName());
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
                assertRunOnEventLoop();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    @Blocking
    public StreamObserver<Messages.StreamingOutputCallRequest> fullDuplexCallBlocking(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        AtomicInteger counter = new AtomicInteger();
        assertRunOnWorker();
        Context ctxt = assertRunOnDuplicatedContext();
        return new StreamObserver<Messages.StreamingOutputCallRequest>() {
            @Override
            public void onNext(Messages.StreamingOutputCallRequest streamingOutputCallRequest) {
                assertRunOnWorker();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                Messages.Payload payload = streamingOutputCallRequest.getPayload();
                ByteString value = ByteString
                        .copyFromUtf8(payload.getBody().toStringUtf8() + counter.incrementAndGet() + "-"
                                + Thread.currentThread().getName());
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
                assertRunOnWorker();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Messages.StreamingOutputCallRequest> halfDuplexCall(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertRunOnEventLoop();
        Context ctxt = assertRunOnDuplicatedContext();
        List<Messages.StreamingOutputCallResponse> list = new CopyOnWriteArrayList<>();
        return new StreamObserver<>() {
            @Override
            public void onNext(Messages.StreamingOutputCallRequest streamingOutputCallRequest) {
                assertRunOnEventLoop();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                String payload = streamingOutputCallRequest.getPayload().getBody().toStringUtf8();
                ByteString value = ByteString.copyFromUtf8(payload.toUpperCase() + "-" + Thread.currentThread().getName());
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
                assertRunOnEventLoop();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                list.forEach(responseObserver::onNext);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    @Blocking
    public StreamObserver<Messages.StreamingOutputCallRequest> halfDuplexCallBlocking(
            StreamObserver<Messages.StreamingOutputCallResponse> responseObserver) {
        assertRunOnWorker();
        Context ctxt = assertRunOnDuplicatedContext();
        List<Messages.StreamingOutputCallResponse> list = new CopyOnWriteArrayList<>();
        return new StreamObserver<>() {
            @Override
            public void onNext(Messages.StreamingOutputCallRequest streamingOutputCallRequest) {
                assertRunOnWorker();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                String payload = streamingOutputCallRequest.getPayload().getBody().toStringUtf8();
                ByteString value = ByteString.copyFromUtf8(payload.toUpperCase() + "-" + Thread.currentThread().getName());
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
                assertRunOnWorker();
                assertRunOnDuplicatedContext();
                assertThat(ctxt).isEqualTo(Vertx.currentContext());
                list.forEach(responseObserver::onNext);
                responseObserver.onCompleted();
            }
        };
    }
}
