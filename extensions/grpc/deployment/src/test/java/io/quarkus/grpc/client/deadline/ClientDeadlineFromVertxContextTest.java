package io.quarkus.grpc.client.deadline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

/**
 * Tests that deadlines set programmatically via {@code withDeadlineAfter} are enforced
 * when the gRPC call is initiated from a Vert.x context (both root and duplicated).
 */
public class ClientDeadlineFromVertxContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, MyConsumer.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer consumer;

    @Test
    public void testDeadlineFromRootContext() {
        Status.Code code = consumer.callWithDeadlineFromRootContext();
        assertThat(code).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
    }

    @Test
    public void testDeadlineFromDuplicatedContext() {
        Status.Code code = consumer.callWithDeadlineFromDuplicatedContext();
        assertThat(code).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient("hello-service")
        MutinyGreeterGrpc.MutinyGreeterStub stub;

        @Inject
        Vertx vertx;

        public Status.Code callWithDeadlineFromRootContext() {
            Context rootContext = vertx.getOrCreateContext();
            return stub.withDeadlineAfter(300, TimeUnit.MILLISECONDS)
                    .sayHello(HelloRequest.newBuilder().setName("root").build())
                    .map(HelloReply::getMessage)
                    .map(ignored -> Status.Code.OK)
                    .onFailure().recoverWithItem(MyConsumer::extractCode)
                    .runSubscriptionOn(rootContext::runOnContext)
                    .await().atMost(Duration.ofSeconds(1));
        }

        public Status.Code callWithDeadlineFromDuplicatedContext() {
            Context root = vertx.getOrCreateContext();
            io.vertx.core.Context duplicate = VertxContext.getOrCreateDuplicatedContext(root.getDelegate());
            return stub.withDeadlineAfter(300, TimeUnit.MILLISECONDS)
                    .sayHello(HelloRequest.newBuilder().setName("duplicated").build())
                    .map(HelloReply::getMessage)
                    .map(ignored -> Status.Code.OK)
                    .onFailure().recoverWithItem(MyConsumer::extractCode)
                    .runSubscriptionOn(MutinyHelper.executor(duplicate))
                    .await().atMost(Duration.ofSeconds(1));
        }

        private static Status.Code extractCode(Throwable t) {
            while (t != null) {
                if (t instanceof StatusRuntimeException) {
                    return ((StatusRuntimeException) t).getStatus().getCode();
                }
                t = t.getCause();
            }
            return Status.Code.UNKNOWN;
        }
    }
}
