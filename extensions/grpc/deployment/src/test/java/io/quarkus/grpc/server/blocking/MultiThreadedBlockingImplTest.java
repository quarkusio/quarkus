package io.quarkus.grpc.server.blocking;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.acme.Request;
import org.acme.Response;
import org.acme.StandardBlockingGrpcServiceGrpc;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.impl.ConcurrentHashSet;

public class MultiThreadedBlockingImplTest {

    private static final Logger logger = Logger.getLogger(GrpcServices.class);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addPackage(StandardBlocking.class.getPackage())
                            .addPackage(
                                    StandardBlockingGrpcServiceGrpc.StandardBlockingGrpcServiceImplBase.class.getPackage()));

    @GrpcClient
    StandardBlockingGrpcServiceGrpc.StandardBlockingGrpcServiceBlockingStub client;

    static ExecutorService executor = Executors.newCachedThreadPool();

    @AfterAll
    static void cleanup() {
        executor.shutdown();
    }

    @Test
    void testTheBlockingCallsCanBeDispatchedOnMultipleThreads() throws InterruptedException {
        int count = 100;
        ConcurrentHashSet<String> threads = new ConcurrentHashSet<>();
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            int id = i;
            executor.submit(() -> {
                threads.add(invokeService(id));
                latch.countDown();
            });
        }

        Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS));
        Assertions.assertTrue(threads.size() > 1);
    }

    String invokeService(int id) {
        return client.invoke(Request.newBuilder().setId(id).build()).getThread();
    }

    @GrpcService
    @Blocking
    static class StandardBlocking extends StandardBlockingGrpcServiceGrpc.StandardBlockingGrpcServiceImplBase {
        @Override
        public void invoke(Request request, StreamObserver<Response> responseObserver) {
            try {
                Thread.sleep(Duration.ofSeconds(2).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            responseObserver.onNext(Response.newBuilder()
                    .setId(request.getId()).setThread(Thread.currentThread().getName()).build());
            responseObserver.onCompleted();
        }
    }
}
