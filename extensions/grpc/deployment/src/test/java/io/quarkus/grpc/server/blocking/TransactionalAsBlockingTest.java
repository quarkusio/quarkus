package io.quarkus.grpc.server.blocking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.dam.Blocking.ThreadName;
import com.dam.Blocking1;
import com.dam.Blocking2;
import com.dam.Blocking3;
import com.dam.MutinyBlocking2Grpc;
import com.dam.MutinyBlocking3Grpc;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

public class TransactionalAsBlockingTest {

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

    @GrpcClient
    Blocking3 client3;

    @Test
    void shouldNotOffloadNonBlockingTransactional() {
        Uni<ThreadName> reply = client1.returnThread1(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("eventloop");
    }

    @Test
    void shouldOffloadTransactionalMethod() {
        Uni<ThreadName> reply = client1.returnThread2(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("executor-thread");
    }

    @Test
    void shouldOffloadTransactionalOnClass() {
        Uni<ThreadName> reply = client2.returnThread1(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("executor-thread");
    }

    @Test
    void shouldNotOffloadNonBlockingMethodOnTransacactionalClass() {
        Uni<ThreadName> reply = client2.returnThread2(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("eventloop");
    }

    @Test
    void shouldNotOffloadTransactionalClassWithNonBlocking() {
        Uni<ThreadName> reply = client3.returnThread1(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("eventloop");
    }

    @Test
    void shouldOffloadBlockingMethodRegardlessOfClassAnnotations() {
        Uni<ThreadName> reply = client3.returnThread2(EMPTY);
        ThreadName threadName = reply.await().atMost(Duration.ofSeconds(10));
        assertThat(threadName.getName()).contains("executor-thread");
    }

    @GrpcService
    public static class Blocking1Service implements Blocking1 {
        @Override
        @Transactional
        @NonBlocking
        public Uni<ThreadName> returnThread1(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }

        @Override
        @Transactional
        public Uni<ThreadName> returnThread2(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }
    }

    @GrpcService
    @Transactional
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

    @GrpcService
    @NonBlocking
    @Transactional
    public static class Blocking3Service extends MutinyBlocking3Grpc.Blocking3ImplBase {
        @Override
        public Uni<ThreadName> returnThread1(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }

        @Override
        @Blocking
        public Uni<ThreadName> returnThread2(com.dam.Blocking.Empty request) {
            String message = Thread.currentThread().getName();
            return Uni.createFrom().item(
                    ThreadName.newBuilder().setName(message).build());
        }
    }
}
