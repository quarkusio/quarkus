package io.quarkus.grpc.server.requestcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.supports.context.GrpcRequestContextGrpcInterceptor;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the unified server, a route registered before the gRPC handler may have activated the request context
 * already; the gRPC request context interceptor must then leave it alone and not log a warning.
 */
public class RequestContextAlreadyActiveTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, RequestContextActivatingRoute.class))
            .withConfigurationResource("hello-config.properties")
            .setLogRecordPredicate(
                    record -> GrpcRequestContextGrpcInterceptor.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertTrue(records.isEmpty(),
                    () -> "Unexpected records: " + records.stream().map(LogRecord::getMessage).toList()));

    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub stub;

    @Test
    public void callSucceedsWithoutWarning() {
        assertEquals("Hello neo", stub.sayHello(HelloRequest.newBuilder().setName("neo").build()).getMessage());
        assertEquals("Hello trinity", stub.sayHello(HelloRequest.newBuilder().setName("trinity").build()).getMessage());
    }
}
