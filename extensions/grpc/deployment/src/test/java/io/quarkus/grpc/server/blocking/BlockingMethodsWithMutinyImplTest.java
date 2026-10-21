package io.quarkus.grpc.server.blocking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.StatusRuntimeException;
import io.grpc.testing.integration.Messages;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.blocking.BlockingTestServiceGrpc;
import io.quarkus.grpc.blocking.MutinyBlockingTestServiceGrpc;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.BlockingMutinyTestService;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.timeouts.TestTimeouts;
import io.quarkus.test.timeouts.TimeLimit;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class BlockingMethodsWithMutinyImplTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(EmptyProtos.class.getPackage())
                            .addPackage(Messages.class.getPackage())
                            .addPackage(BlockingTestServiceGrpc.class.getPackage())
                            .addClasses(BlockingMutinyTestService.class, AssertHelper.class))
            .withConfigurationResource("blocking-test-config.properties");

    @GrpcClient("blocking-test")
    BlockingTestServiceGrpc.BlockingTestServiceBlockingStub service;

    @GrpcClient("blocking-test")
    MutinyBlockingTestServiceGrpc.MutinyBlockingTestServiceStub mutiny;

    @Test
    @TimeLimit
    public void testEmpty() {
        EmptyProtos.Empty empty = service
                .emptyCall(EmptyProtos.Empty.newBuilder().build());
        assertThat(empty).isNotNull();
    }

    @Test
    @TimeLimit
    public void testBlockingEmpty() {
        EmptyProtos.Empty empty = service
                .emptyCallBlocking(EmptyProtos.Empty.newBuilder().build());
        assertThat(empty).isNotNull();
    }

    @Test
    @TimeLimit
    public void testUnaryMethod() {
        Messages.SimpleResponse response = service
                .unaryCall(Messages.SimpleRequest.newBuilder().build());
        assertThat(response).isNotNull();
    }

    @Test
    @TimeLimit
    public void testUnaryMethodBlocking() {
        Messages.SimpleResponse response = service
                .unaryCallBlocking(Messages.SimpleRequest.newBuilder().build());
        assertThat(response).isNotNull();
    }

    @Test
    @TimeLimit
    public void testStreamingOutMethod() {
        Iterator<Messages.StreamingOutputCallResponse> iterator = service
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
    @TimeLimit
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
                .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    @TimeLimit
    public void testStreamingInMethodWithMutinyClient() {
        Multi<Messages.StreamingInputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingInputCallRequest.newBuilder().setPayload(p).build());
        Uni<Messages.StreamingInputCallResponse> done = mutiny.streamingInputCall(input);
        assertThat(done).isNotNull();
        done.await().atMost(TestTimeouts.get());
    }

    @Test
    @TimeLimit
    public void testStreamingInMethodBlockingWithMutinyClient() {
        Multi<Messages.StreamingInputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingInputCallRequest.newBuilder().setPayload(p).build());
        Uni<Messages.StreamingInputCallResponse> done = mutiny.streamingInputCallBlocking(input);
        assertThat(done).isNotNull();
        done.await().atMost(TestTimeouts.get());
    }

    @Test
    @TimeLimit
    public void testFullDuplexMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.fullDuplexCall(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TestTimeouts.get());
        assertThat(response).isNotNull().hasSize(4)
                .containsExactly("a1", "b2", "c3", "d4");
    }

    @Test
    @TimeLimit
    public void testFullDuplexMethodBlockingWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.fullDuplexCallBlocking(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TestTimeouts.get());
        assertThat(response).isNotNull().hasSize(4)
                .containsExactly("a1", "b2", "c3", "d4");
    }

    @Test
    @TimeLimit
    public void testHalfDuplexMethodWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.halfDuplexCall(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TestTimeouts.get());
        assertThat(response).isNotNull().hasSize(4).containsExactly("A", "B", "C", "D");

    }

    @Test
    @TimeLimit
    public void testHalfDuplexMethodBlockingWithMutinyClient() {
        Multi<Messages.StreamingOutputCallRequest> input = Multi.createFrom().items("a", "b", "c", "d")
                .map(s -> Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(s)).build())
                .map(p -> Messages.StreamingOutputCallRequest.newBuilder().setPayload(p).build());
        List<String> response = mutiny.halfDuplexCallBlocking(input)
                .map(o -> o.getPayload().getBody().toStringUtf8())
                .collect().asList()
                .await().atMost(TestTimeouts.get());
        assertThat(response).isNotNull().hasSize(4)
                .containsExactly("A", "B", "C", "D");
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
