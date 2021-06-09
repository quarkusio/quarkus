package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.MutinyTestServiceGrpc;
import io.grpc.testing.integration.TestServiceGrpc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class GrpcServiceTestBase {

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);
    protected ManagedChannel channel;

    @BeforeEach
    public void init() throws Exception {
        channel = ManagedChannelBuilder.forAddress("localhost", 9001)
                .usePlaintext()
                .build();
    }

    @AfterEach
    public void shutdown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    public void testHelloWithBlockingClient() {
        HelloReply reply = GreeterGrpc.newBlockingStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo");
    }

    @Test
    public void testHelloWithMutinyClient() {
        Uni<HelloReply> reply = MutinyGreeterGrpc.newMutinyStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.await().atMost(TIMEOUT).getMessage()).isEqualTo("Hello neo");
    }

    @Test
    public void testEmptyWithBlockingClient() {
        EmptyProtos.Empty empty = TestServiceGrpc.newBlockingStub(channel)
                .emptyCall(EmptyProtos.Empty.newBuilder().build());
        assertThat(empty).isNotNull();
    }

    @Test
    public void testEmptyWithMutinyClient() {
        EmptyProtos.Empty empty = MutinyTestServiceGrpc.newMutinyStub(channel)
                .emptyCall(EmptyProtos.Empty.newBuilder().build()).await().atMost(TIMEOUT);
        assertThat(empty).isNotNull();
    }

    @Test
    public void testUnaryMethodWithBlockingClient() {
        Messages.SimpleResponse response = TestServiceGrpc.newBlockingStub(channel)
                .unaryCall(Messages.SimpleRequest.newBuilder().build());
        assertThat(response).isNotNull();
    }

    @Test
    public void testUnaryMethodWithMutinyClient() {
        Messages.SimpleResponse response = MutinyTestServiceGrpc.newMutinyStub(channel)
                .unaryCall(Messages.SimpleRequest.newBuilder().build()).await().atMost(TIMEOUT);
        assertThat(response).isNotNull();
    }

    @Test
    public void testStreamingOutMethodWithBlockingClient() {
        Iterator<Messages.StreamingOutputCallResponse> iterator = TestServiceGrpc
                .newBlockingStub(channel)
                .streamingOutputCall(Messages.StreamingOutputCallRequest.newBuilder().build());
        assertThat(iterator).isNotNull();
        List<String> list = new CopyOnWriteArrayList<>();
        iterator.forEachRemaining(so -> {
            String content = so.getPayload().getBody().toStringUtf8();
            list.add(content);
        });
        assertThat(list).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testStreamingOutMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallResponse> multi = MutinyTestServiceGrpc
                .newMutinyStub(channel)
                .streamingOutputCall(Messages.StreamingOutputCallRequest.newBuilder().build());
        assertThat(multi).isNotNull();
        List<String> list = multi.map(o -> o.getPayload().getBody().toStringUtf8()).collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(list).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testStreamingInMethodWithMutinyClient() {
        Multi<Messages.StreamingInputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingInputCallRequest.newBuilder().setPayload(p).build());
        Uni<Messages.StreamingInputCallResponse> done = MutinyTestServiceGrpc
                .newMutinyStub(channel).streamingInputCall(input);
        assertThat(done).isNotNull();
        done.await().atMost(TIMEOUT);
    }

    @Test
    public void testFullDuplexMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = MutinyTestServiceGrpc
                .newMutinyStub(channel).fullDuplexCall(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(response).isNotNull();
        assertThat(response).containsExactly("a1", "b2", "c3", "d4");
    }

    @Test
    public void testHalfDuplexMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = MutinyTestServiceGrpc
                .newMutinyStub(channel).halfDuplexCall(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(response).isNotNull();
        assertThat(response).containsExactly("A", "B", "C", "D");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testUnimplementedMethodWithBlockingClient() {
        assertThatThrownBy(
                () -> TestServiceGrpc.newBlockingStub(channel).unimplementedCall(EmptyProtos.Empty.newBuilder().build()))
                        .isInstanceOf(StatusRuntimeException.class).hasMessageContaining("UNIMPLEMENTED");
    }

    @Test
    public void testUnimplementedMethodWithMutinyClient() {
        assertThatThrownBy(
                () -> MutinyTestServiceGrpc.newMutinyStub(channel).unimplementedCall(EmptyProtos.Empty.newBuilder().build())
                        .await().atMost(TIMEOUT)).isInstanceOf(StatusRuntimeException.class)
                                .hasMessageContaining("UNIMPLEMENTED");
    }

    @Test
    public void testHealth() {
        HealthOuterClass.HealthCheckResponse check = HealthGrpc.newBlockingStub(channel)
                .check(HealthOuterClass.HealthCheckRequest.newBuilder().build());
        assertThat(check.getStatus()).isEqualTo(HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING);
    }
}
