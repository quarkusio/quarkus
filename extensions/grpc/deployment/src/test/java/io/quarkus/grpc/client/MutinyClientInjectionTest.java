package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.WorkerContext;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;

public class MutinyClientInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(HelloService.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer service;

    @Test
    public void test() {
        String neo = service.invoke("neo-mutiny");
        assertThat(neo).matches("Hello neo-mutiny");

        String fromIO = service.invokeFromIoThread("neo-io");
        assertThat(fromIO).matches("Hello neo-io");

        String fromDC = service.invokeFromDuplicatedContext("neo-dc");
        assertThat(fromDC).matches("Hello neo-dc");
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient("hello-service")
        Greeter service;

        @Inject
        Vertx vertx;

        public String invoke(String s) {
            return service.sayHello(HelloRequest.newBuilder().setName(s).build())
                    .map(HelloReply::getMessage)
                    .invoke(() -> assertThat(Vertx.currentContext()).isNull())
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
                            .subscribe().with(e::complete, e::fail);
                });
            }).await().atMost(Duration.ofSeconds(5));
        }

        public String invokeFromDuplicatedContext(String s) {
            Context root = vertx.getOrCreateContext();
            ContextInternal duplicate = (ContextInternal) VertxContext.getOrCreateDuplicatedContext(root.getDelegate());
            return Uni.createFrom().<String> emitter(e -> {
                duplicate.runOnContext(x -> {
                    service.sayHello(HelloRequest.newBuilder().setName(s).build())
                            .map(HelloReply::getMessage)
                            .invoke(() -> assertThat(Vertx.currentContext().getDelegate())
                                    .isNotInstanceOf(EventLoopContext.class).isNotInstanceOf(WorkerContext.class)
                                    .isEqualTo(duplicate))
                            .subscribe().with(e::complete, e::fail);
                });
            }).await().atMost(Duration.ofSeconds(5));
        }

    }
}
