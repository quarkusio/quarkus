package io.quarkus.grpc.server.blocking.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.test.blocking.inheritance.BlockingTestService;
import io.quarkus.grpc.test.blocking.inheritance.BlockingTestServiceGrpc;
import io.quarkus.grpc.test.blocking.inheritance.InheritenceTest.Msg;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class ClassLevelBlockingTest {
    private static final String BLOCKING = "blocking";
    private static final String NON_BLOCKING = "nonblocking";
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(BlockingTestServiceGrpc.class.getPackage()).addClasses(BlockingTestService.class));

    @GrpcClient
    BlockingTestService client;

    @Test
    @Timeout(5)
    void shouldBlockOnInheritedClassLevelBlocking() {
        assertThat(client.notOverridden1(message()).await().indefinitely().getText()).isEqualTo(BLOCKING);
    }

    @Test
    @Timeout(5)
    void shouldNotBlockOnInheritedMethodLevelNonBlocking() {
        assertThat(client.notOverridden2(message()).await().indefinitely().getText()).isEqualTo(NON_BLOCKING);
    }

    @Test
    @Timeout(5)
    void shouldNotBlockOnClassLevelOverriddenBlocking() {
        assertThat(client.overridden1(message()).await().indefinitely().getText()).isEqualTo(NON_BLOCKING);
    }

    @Test
    @Timeout(5)
    void shouldNotBlockOnOverridden() {
        assertThat(client.overridden2(message()).await().indefinitely().getText()).isEqualTo(NON_BLOCKING);
    }

    private Msg message() {
        return Msg.newBuilder().setText("foo").build();
    }

    @Blocking
    public static class ServiceA implements BlockingTestService {
        @Override
        public Uni<Msg> overridden1(Msg request) {
            return isBlocking();
        }

        @Override
        public Uni<Msg> overridden2(Msg request) {
            return isBlocking();
        }

        @Override
        public Uni<Msg> notOverridden1(Msg request) {
            return isBlocking();
        }

        @Override
        @NonBlocking
        public Uni<Msg> notOverridden2(Msg request) {
            return isBlocking();
        }

        Uni<Msg> isBlocking() {
            boolean isEventLoop = Thread.currentThread().getName().contains("eventloop");
            return Uni.createFrom().item(isEventLoop ? NON_BLOCKING : BLOCKING)
                    .map(text -> Msg.newBuilder().setText(text).build());
        }
    }

    @NonBlocking
    public static class ServiceB extends ServiceA {
        @Override
        public Uni<Msg> overridden1(Msg request) {
            return isBlocking();
        }
    }

    @GrpcService
    public static class ServiceC extends ServiceB {
        @Override
        public Uni<Msg> overridden2(Msg request) {
            return isBlocking();
        }
    }
}
