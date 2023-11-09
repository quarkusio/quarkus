package org.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
class GrpcHelloServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutiny;

    Traces traces;

    @BeforeEach
    @AfterEach
    void setUp() {
        traces.reset();
    }

    @Test
    public void testHelloWorldServiceUsingBlockingStub() throws InterruptedException {
        HelloReply reply = mutiny
                .sayHello(HelloRequest.newBuilder().setName("neo-blocking").build())
                .await().atMost(TIMEOUT);
        Assertions.assertEquals(reply.getMessage(), "Hello neo-blocking");
        Thread.sleep(1000);
        verifyTraces();
    }
    //
    //
    //    @Test
    //    void test() {
    //        verifyHttpResponse();
    //        verifyTraces();
    //    }

    //    private void verifyHttpResponse() {
    //        when()
    //                .get("/hello")
    //                .then()
    //                .statusCode(200);
    //    }

    private void verifyTraces() {
        await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(traces.getTraceRequests()).hasSize(1));

        ExportTraceServiceRequest request = traces.getTraceRequests().get(0);
        assertThat(request.getResourceSpansCount()).isEqualTo(1);

        ResourceSpans resourceSpans = request.getResourceSpans(0);
        assertThat(resourceSpans.getResource().getAttributesList())
                //                .contains(
                //                        KeyValue.newBuilder()
                //                                .setKey(ResourceAttributes.SERVICE_NAME.getKey())
                //                                .setValue(AnyValue.newBuilder()
                //                                        .setStringValue("integration test").build())
                //                                .build())
                .contains(
                        KeyValue.newBuilder()
                                .setKey(ResourceAttributes.WEBENGINE_NAME.getKey())
                                .setValue(AnyValue.newBuilder()
                                        .setStringValue("Quarkus").build())
                                .build());
        assertThat(resourceSpans.getScopeSpansCount()).isEqualTo(1);
        ScopeSpans scopeSpans = resourceSpans.getScopeSpans(0);
        assertThat(scopeSpans.getSpansCount()).isEqualTo(2);
        //        Span span = scopeSpans.getSpans(0);
        ////        assertThat(span.getName()).isEqualTo("GET /hello");
        //        assertThat(span.getAttributesList())
        //                .contains(
        //                        KeyValue.newBuilder()
        //                                .setKey("http.method")
        //                                .setValue(AnyValue.newBuilder()
        //                                        .setStringValue("GET").build())
        //                                .build());
    }
}
