package io.quarkus.grpc.server.blocking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.dam.Blocking.ThreadName;
import com.dam.Blocking1;
import com.dam.Blocking2;
import com.dam.MutinyBlocking2Grpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class BlockingOnClassTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(Blocking1.class.getPackage())
                            .addClasses(Blocking1Service.class, Blocking2Service.class));
    public static final com.dam.Blocking.Empty EMPTY = com.dam.Blocking.Empty.newBuilder().getDefaultInstanceForType();

    @GrpcClient
    Blocking1 client1;

    @GrpcClient
    Blocking2 client2;

    @Test
    void shouldOffloadBlockingOnInterfaceImplementation() {
        Uni<ThreadName> reply = client1.returnThread1(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("executor-thread");
    }

    @Test
    void shouldNotOffloadNonBlockingOnInterfaceImplementation() {
        Uni<ThreadName> reply = client1.returnThread2(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("eventloop");
    }

    @Test
    void shouldOffloadBlockingOnSuperclassImplementation() {
        Uni<ThreadName> reply = client2.returnThread1(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("executor-thread");
    }

    @Test
    void shouldNotOffloadNonBlockingOnSuperclassImplementation() {
        Uni<ThreadName> reply = client2.returnThread2(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("eventloop");
    }

    @GrpcService
    @Blocking
    public static class Blocking1Service implements Blocking1 {
        @Override
        public Uni<ThreadName> returnThread1(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }

        @Override
        @NonBlocking
        public Uni<ThreadName> returnThread2(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }
    }

    @GrpcService
    @Blocking
    public static class Blocking2Service extends MutinyBlocking2Grpc.Blocking2ImplBase {
        @Override
        public Uni<ThreadName> returnThread1(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }

        @Override
        @NonBlocking
        public Uni<ThreadName> returnThread2(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }
    }
}
