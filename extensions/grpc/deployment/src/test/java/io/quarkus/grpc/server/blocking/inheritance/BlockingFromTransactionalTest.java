package io.quarkus.grpc.server.blocking.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.test.blocking.inheritance.BlockingTestService;
import io.quarkus.grpc.test.blocking.inheritance.BlockingTestServiceGrpc;
import io.quarkus.grpc.test.blocking.inheritance.InheritenceTest;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class BlockingFromTransactionalTest {
    private static final String BLOCKING = "blocking";
    private static final String NON_BLOCKING = "nonblocking";
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(BlockingTestServiceGrpc.class.getPackage())
                            .addClasses(BlockingTestService.class));

    @GrpcClient
    BlockingTestService client;

    @Test
    @Timeout(5)
    void shouldBlockOnInheritedClassLevelTransactional() {
        assertThat(client.notOverridden1(message()).await().indefinitely().getText()).isEqualTo(BLOCKING);
    }

    @Test
    @Timeout(5)
    void shouldNotBlockOnInheritedTransactionalMarkedNonBlocking() {
        assertThat(client.notOverridden2(message()).await().indefinitely().getText()).isEqualTo(NON_BLOCKING);
    }

    @Test
    @Timeout(5)
    void shouldNotBlockOnOverriddenTransactionalMarkedNonBlocking() {
        assertThat(client.overridden1(message()).await().indefinitely().getText()).isEqualTo(NON_BLOCKING);
    }

    @Test
    @Timeout(5)
    void shouldBlockOnOverriddenTransactional() {
        assertThat(client.overridden2(message()).await().indefinitely().getText()).isEqualTo(BLOCKING);
    }

    private InheritenceTest.Msg message() {
        return InheritenceTest.Msg.newBuilder().setText("foo").build();
    }

    @Transactional
    public static class ServiceA implements BlockingTestService {
        @Override
        public Uni<InheritenceTest.Msg> overridden1(InheritenceTest.Msg request) {
            return isBlocking();
        }

        @Override
        public Uni<InheritenceTest.Msg> overridden2(InheritenceTest.Msg request) {
            return isBlocking();
        }

        @Override
        public Uni<InheritenceTest.Msg> notOverridden1(InheritenceTest.Msg request) {
            return isBlocking();
        }

        @Override
        @NonBlocking
        public Uni<InheritenceTest.Msg> notOverridden2(InheritenceTest.Msg request) {
            return isBlocking();
        }

        Uni<InheritenceTest.Msg> isBlocking() {
            boolean isEventLoop = Thread.currentThread().getName().contains("eventloop");
            return Uni.createFrom().item(isEventLoop ? NON_BLOCKING : BLOCKING)
                    .map(text -> InheritenceTest.Msg.newBuilder().setText(text).build());
        }
    }

    @GrpcService
    public static class ServiceB extends ServiceA {
        @Override
        @NonBlocking
        public Uni<InheritenceTest.Msg> overridden1(InheritenceTest.Msg request) {
            return isBlocking();
        }

        @Override
        public Uni<InheritenceTest.Msg> overridden2(InheritenceTest.Msg request) {
            return isBlocking();
        }
    }
}
