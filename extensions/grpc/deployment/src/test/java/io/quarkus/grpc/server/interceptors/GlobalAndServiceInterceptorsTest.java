package io.quarkus.grpc.server.interceptors;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.examples.goodbyeworld.Farewell;
import io.grpc.examples.goodbyeworld.FarewellGrpc;
import io.grpc.examples.goodbyeworld.GoodbyeReply;
import io.grpc.examples.goodbyeworld.GoodbyeRequest;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterBean;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld3.Greeter3;
import io.grpc.examples.helloworld3.Greeter3Grpc;
import io.grpc.examples.helloworld3.HelloReply3;
import io.grpc.examples.helloworld3.HelloRequest3;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class GlobalAndServiceInterceptorsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addPackage(Greeter3Grpc.class.getPackage())
                    .addPackage(FarewellGrpc.class.getPackage())
                    .addClasses(MyFirstInterceptor.class, GreeterBean.class, HelloRequest.class));

    protected ManagedChannel channel;

    @GrpcClient
    Greeter greeter;

    @GrpcClient
    Greeter3 greeter3;

    @GrpcClient
    Farewell farewell;

    @BeforeEach
    void cleanUp() {
        config.getLogRecords();
        GlobalInterceptor.invoked = false;
        ServiceBInterceptor.invoked = false;
        FarewellInterceptor.invoked = false;
    }

    @Test
    void shouldInvokeGlobalInterceptorAndNotInvokedUnregisteredLocal() {
        Uni<HelloReply> result = greeter.sayHello(HelloRequest.newBuilder().setName("ServiceA").build());

        HelloReply helloReply = result.await().atMost(Duration.ofSeconds(5));
        assertThat(helloReply.getMessage()).isEqualTo("Hello, ServiceA");

        assertThat(GlobalInterceptor.invoked).isTrue();
        assertThat(ServiceBInterceptor.invoked).isFalse();
        assertThat(FarewellInterceptor.invoked).isFalse();
    }

    @Test
    void shouldInvokeGlobalInterceptorAndInvokedRegisteredLocal() {
        Uni<HelloReply3> result = greeter3.sayHello(HelloRequest3.newBuilder().setName("ServiceB").build());

        HelloReply3 helloReply = result.await().atMost(Duration.ofSeconds(5));
        assertThat(helloReply.getMessage()).isEqualTo("Hello3, ServiceB");

        assertThat(GlobalInterceptor.invoked).isTrue();
        assertThat(ServiceBInterceptor.invoked).isTrue();
        assertThat(FarewellInterceptor.invoked).isFalse();
    }

    @Test
    void shouldInvokeGlobalInterceptorAndInvokedRegisteredLocalOnGrpcStub() {
        Uni<GoodbyeReply> result = farewell.sayGoodbye(GoodbyeRequest.newBuilder().setName("Farewell").build());

        GoodbyeReply goodbyeReply = result.await().atMost(Duration.ofSeconds(5));
        assertThat(goodbyeReply.getMessage()).isEqualTo("Goodbye, Farewell");

        assertThat(GlobalInterceptor.invoked).isTrue();
        assertThat(ServiceBInterceptor.invoked).isFalse();
        assertThat(FarewellInterceptor.invoked).isTrue();
    }

    @GrpcService
    public static class ServiceA implements Greeter {
        @Override
        public Uni<HelloReply> sayHello(HelloRequest request) {
            return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hello, " + request.getName()).build());
        }
    }

    @GrpcService
    @RegisterInterceptor(ServiceBInterceptor.class)
    public static class ServiceB implements Greeter3 {
        @Override
        public Uni<HelloReply3> sayHello(HelloRequest3 request) {
            return Uni.createFrom().item(HelloReply3.newBuilder().setMessage("Hello3, " + request.getName()).build());
        }
    }

    @io.quarkus.grpc.GlobalInterceptor
    @ApplicationScoped
    public static class GlobalInterceptor implements ServerInterceptor {
        static boolean invoked;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            invoked = true;
            return next.startCall(call, headers);
        }
    }

    @GrpcService
    @RegisterInterceptor(FarewellInterceptor.class)
    public static class FarewellService extends FarewellGrpc.FarewellImplBase {
        @Override
        public void sayGoodbye(GoodbyeRequest request, StreamObserver<GoodbyeReply> responseObserver) {
            responseObserver.onNext(GoodbyeReply.newBuilder().setMessage("Goodbye, " + request.getName()).build());
            responseObserver.onCompleted();
        }
    }

    @ApplicationScoped
    public static class ServiceBInterceptor implements ServerInterceptor {
        static boolean invoked;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            invoked = true;
            return next.startCall(call, headers);
        }
    }

    @ApplicationScoped
    public static class FarewellInterceptor implements ServerInterceptor {
        static boolean invoked;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            invoked = true;
            return next.startCall(call, headers);
        }
    }

    @ApplicationScoped
    public static class UnusedInterceptor implements ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            throw new IllegalStateException("Interceptor that should not be called was invoked");
        }
    }
}
