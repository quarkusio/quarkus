package io.quarkus.grpc.server.interceptors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.assertj.core.api.Condition;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterBean;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ClosingCallInInterceptorTest {

    private static final Metadata.Key<String> CLOSE_REASON_KEY = Metadata.Key.of("CUSTOM_CLOSE_REASON",
            Metadata.ASCII_STRING_MARSHALLER);
    private static final String STATED_REASON_TO_CLOSE = "Because I want to close it.";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(MyClosingCallInterceptor.class, GreeterBean.class, HelloRequest.class, HelloService.class)
                    .addAsResource(new StringAsset("quarkus.grpc.server.use-separate-server=false" + System.lineSeparator()),
                            "application.properties"))
            .setLogRecordPredicate(
                    record -> record.getMessage() != null && record.getMessage().contains("Closing gRPC call due to an error"))
            .assertLogRecords(logRecords -> {
                if (!logRecords.isEmpty()) {
                    for (LogRecord logRecord : logRecords) {
                        if (logRecord.getThrown() instanceof IllegalStateException ise
                                && ise.getMessage().contains("Already closed")) {
                            Assertions.fail("Log contains message with 'java.lang.IllegalStateException: Already closed'");
                        }
                    }
                }
            });

    @GrpcClient
    Greeter greeter;

    @Test
    void test() {
        Uni<HelloReply> result = greeter.sayHello(HelloRequest.newBuilder().setName("ServiceA").build());
        assertThatThrownBy(() -> result.await().atMost(Duration.ofSeconds(4)))
                .isInstanceOf(StatusRuntimeException.class)
                .has(new Condition<Throwable>(t -> {
                    if (t instanceof StatusRuntimeException statusRuntimeException) {
                        var trailers = statusRuntimeException.getTrailers();
                        if (trailers != null) {
                            return STATED_REASON_TO_CLOSE.equals(trailers.get(CLOSE_REASON_KEY));
                        }
                    }
                    return false;
                }, "Checking close reason returned in metadata"))
                .hasMessageContaining("UNAUTHENTICATED");
    }

    @ApplicationScoped
    @GlobalInterceptor
    public static class MyClosingCallInterceptor implements ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            var metadata = new Metadata();
            metadata.put(CLOSE_REASON_KEY, STATED_REASON_TO_CLOSE);
            call.close(Status.UNAUTHENTICATED, metadata);
            return new ServerCall.Listener<>() {
            };
        }
    }

}
