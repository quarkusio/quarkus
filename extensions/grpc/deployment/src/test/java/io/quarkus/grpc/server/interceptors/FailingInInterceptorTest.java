package io.quarkus.grpc.server.interceptors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.*;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class FailingInInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(MyFailingInterceptor.class, GreeterBean.class, HelloRequest.class, HelloService.class));

    @GrpcClient
    Greeter greeter;

    @Test
    void test() {
        Uni<HelloReply> result = greeter.sayHello(HelloRequest.newBuilder().setName("ServiceA").build());
        assertThatThrownBy(() -> result.await().atMost(Duration.ofSeconds(4)))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @ApplicationScoped
    @GlobalInterceptor
    public static class MyFailingInterceptor implements ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            return next
                    .startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

                        @Override
                        public void sendMessage(RespT message) {
                            throw new IllegalArgumentException("BOOM");
                        }

                        @Override
                        public void close(Status status, Metadata trailers) {
                            super.close(status, trailers);
                        }
                    }, headers);
        }
    }

}
