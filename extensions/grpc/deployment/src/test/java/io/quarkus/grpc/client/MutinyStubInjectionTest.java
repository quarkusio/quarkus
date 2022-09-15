package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.WorkerContext;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class MutinyStubInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyConsumer.class,
                            MutinyGreeterGrpc.class, GreeterGrpc.class,
                            MutinyGreeterGrpc.MutinyGreeterStub.class,
                            HelloService.class, HelloRequest.class, HelloReply.class,
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer service;

    @Test
    public void test() {
        String neo = service.invoke("neo-mutiny");
        assertThat(neo).startsWith("Hello neo-mutiny").doesNotContain("vert.x");

        neo = service.invokeFromIoThread("neo-io");
        assertThat(neo).startsWith("Hello neo-io").contains("vert.x");

        neo = service.invokeFromDuplicatedContext("neo-dc");
        assertThat(neo).startsWith("Hello neo-dc").contains("vert.x");
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient("hello-service")
        MutinyGreeterGrpc.MutinyGreeterStub service;

        @Inject
        Vertx vertx;

        public String invoke(String s) {
            return service.sayHello(HelloRequest.newBuilder().setName(s).build())
                    .map(HelloReply::getMessage)
                    .invoke(() -> assertThat(Vertx.currentContext()).isNull())
                    .map(r -> r + " " + Thread.currentThread().getName())
                    .await().atMost(Duration.ofSeconds(5));
        }

        public String invokeFromIoThread(String s) {
            Context context = vertx.getOrCreateContext();
            return Uni.createFrom().<String> emitter(e -> {
                context.runOnContext(() -> {
                    service.sayHello(HelloRequest.newBuilder().setName(s).build())
                            .map(HelloReply::getMessage)
                            .invoke(() -> assertThat(Vertx.currentContext()).isNotNull().isEqualTo(context))
                            .invoke(() -> assertThat(Vertx.currentContext().getDelegate()).isInstanceOf(EventLoopContext.class))
                            .map(r -> r + " " + Thread.currentThread().getName())
                            .subscribe().with(e::complete, e::fail);
                });
            }).await().atMost(Duration.ofSeconds(5));
        }

        public String invokeFromDuplicatedContext(String s) {
            Context root = vertx.getOrCreateContext();
            io.vertx.core.Context duplicate = VertxContext.getOrCreateDuplicatedContext(root.getDelegate());
            return Uni.createFrom().<String> emitter(e -> {
                duplicate.runOnContext(x -> {
                    service.sayHello(HelloRequest.newBuilder().setName(s).build())
                            .map(HelloReply::getMessage)
                            .invoke(() -> assertThat(Vertx.currentContext().getDelegate())
                                    .isNotInstanceOf(EventLoopContext.class).isNotInstanceOf(WorkerContext.class)
                                    .isEqualTo(duplicate))
                            .map(r -> r + " " + Thread.currentThread().getName())
                            .subscribe().with(e::complete, e::fail);
                });
            }).await().atMost(Duration.ofSeconds(5));
        }

    }
}
