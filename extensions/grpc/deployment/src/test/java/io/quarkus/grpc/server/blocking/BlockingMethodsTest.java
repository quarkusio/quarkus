package io.quarkus.grpc.server.blocking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.StatusRuntimeException;
import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.blocking.BlockingTestServiceGrpc;
import io.quarkus.grpc.blocking.MutinyBlockingTestServiceGrpc;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.BlockingTestService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class BlockingMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(EmptyProtos.class.getPackage())
                            .addPackage(Messages.class.getPackage())
                            .addPackage(BlockingTestServiceGrpc.class.getPackage())
                            .addClasses(BlockingTestService.class, AssertHelper.class))
            .withConfigurationResource("blocking-test-config.properties");

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);

    @GrpcClient("blocking-test")
    BlockingTestServiceGrpc.BlockingTestServiceBlockingStub service;

    @GrpcClient("blocking-test")
    MutinyBlockingTestServiceGrpc.MutinyBlockingTestServiceStub mutiny;

    @Test
    @Timeout(5)
    public void testEmpty() {
        EmptyProtos.Empty empty = service
                .emptyCall(EmptyProtos.Empty.newBuilder().build());
        assertThat(empty).isNotNull();
    }

    @Test
    @Timeout(5)
    public void testBlockingEmpty() {
        EmptyProtos.Empty empty = service
                .emptyCallBlocking(EmptyProtos.Empty.newBuilder().build());
        assertThat(empty).isNotNull();
    }

    @Test
    @Timeout(5)
    public void testUnaryMethod() {
        Messages.SimpleResponse response = service
                .unaryCall(Messages.SimpleRequest.newBuilder().build());
        assertThat(response).isNotNull();
    }

    @Test
    @Timeout(5)
    public void testUnaryMethodBlocking() {
        Messages.SimpleResponse response = service
                .unaryCallBlocking(Messages.SimpleRequest.newBuilder().build());
        assertThat(response).isNotNull();
    }

    @Test
    @Timeout(5)
    public void testStreamingOutMethod() {
        Iterator<Messages.StreamingOutputCallResponse> iterator = service
                .streamingOutputCall(Messages.StreamingOutputCallRequest.newBuilder().build());
        assertThat(iterator).isNotNull();
        List<String> list = new CopyOnWriteArrayList<>();
        iterator.forEachRemaining(so -> {
            String content = so.getPayload().getBody().toStringUtf8();
            list.add(content);
        });
        assertThat(list).hasSize(10)
                .allSatisfy(s -> {
                    assertThat(s).contains("eventloop");
                });
    }

    @Test
    @Timeout(5)
    public void testStreamingOutMethodBlocking() {
        Iterator<Messages.StreamingOutputCallResponse> iterator = service
                .streamingOutputCallBlocking(Messages.StreamingOutputCallRequest.newBuilder().build());
        assertThat(iterator).isNotNull();
        List<String> list = new CopyOnWriteArrayList<>();
        iterator.forEachRemaining(so -> {
            String content = so.getPayload().getBody().toStringUtf8();
            list.add(content);
        });
        assertThat(list).hasSize(10)
                .allSatisfy(s -> {
                    assertThat(s).contains("executor");
                });
    }

    @Test
    @Timeout(5)
    public void testStreamingInMethodWithMutinyClient() {
        Multi<Messages.StreamingInputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingInputCallRequest.newBuilder().setPayload(p).build());
        Uni<Messages.StreamingInputCallResponse> done = mutiny.streamingInputCall(input);
        assertThat(done).isNotNull();
        done.await().atMost(TIMEOUT);
    }

    @Test
    @Timeout(5)
    public void testStreamingInMethodBlockingWithMutinyClient() {
        Multi<Messages.StreamingInputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingInputCallRequest.newBuilder().setPayload(p).build());
        Uni<Messages.StreamingInputCallResponse> done = mutiny.streamingInputCallBlocking(input);
        assertThat(done).isNotNull();
        done.await().atMost(TIMEOUT);
    }

    @Test
    @Timeout(5)
    public void testFullDuplexMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.fullDuplexCall(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(response).isNotNull().hasSize(4)
                .allSatisfy(s -> {
                    assertThat(s).contains("eventloop");
                });
    }

    @Test
    @Timeout(5)
    public void testFullDuplexMethodBlockingWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.fullDuplexCallBlocking(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(response).isNotNull().hasSize(4)
                .allSatisfy(s -> {
                    assertThat(s).contains("executor");
                });
    }

    @Test
    public void testHalfDuplexMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.halfDuplexCall(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(response).isNotNull().hasSize(4)
                .allSatisfy(s -> {
                    assertThat(s).contains("eventloop");
                });
    }

    @Test
    public void testHalfDuplexMethodBlockingWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.halfDuplexCallBlocking(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TIMEOUT);
        assertThat(response).isNotNull().hasSize(4)
                .allSatisfy(s -> {
                    assertThat(s).contains("executor");
                });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testUnimplementedMethod() {
        Assertions.assertThatThrownBy(
                () -> service
                        .unimplementedCall(EmptyProtos.Empty.newBuilder().build()))
                .isInstanceOf(StatusRuntimeException.class).hasMessageContaining("UNIMPLEMENTED");

        Assertions.assertThatThrownBy(
                () -> service
                        .unimplementedCallBlocking(EmptyProtos.Empty.newBuilder().build()))
                .isInstanceOf(StatusRuntimeException.class).hasMessageContaining("UNIMPLEMENTED");
    }

}
