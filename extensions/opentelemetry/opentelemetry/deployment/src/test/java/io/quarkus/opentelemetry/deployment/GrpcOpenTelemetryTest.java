package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_PORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_TRANSPORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_GRPC_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_SYSTEM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class GrpcOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestSpanExporter.class)
                    .addClasses(HelloService.class)
                    .addClasses(GreeterGrpc.class, MutinyGreeterGrpc.class,
                            Greeter.class, GreeterBean.class, GreeterClient.class,
                            HelloProto.class, HelloRequest.class, HelloRequestOrBuilder.class,
                            HelloReply.class, HelloReplyOrBuilder.class)
                    .addClasses(StreamService.class)
                    .addClasses(StreamingGrpc.class, MutinyStreamingGrpc.class,
                            Streaming.class, StreamingBean.class, StreamingClient.class,
                            StreamingProto.class, Item.class, ItemOrBuilder.class));

    @Inject
    TestSpanExporter spanExporter;
    @GrpcClient
    MutinyGreeterGrpc.MutinyGreeterStub greeterStub;
    @Inject
    HelloBean helloBean;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void grpc() {
        String response = greeterStub.sayHello(
                HelloRequest.newBuilder().setName("Naruto").build())
                .map(HelloReply::getMessage)
                .await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello Naruto", response);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals(2, spans.size());

        SpanData server = spans.get(0);
        assertEquals("helloworld.Greeter/SayHello", server.getName());
        assertEquals(SpanKind.SERVER, server.getKind());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", server.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(NET_PEER_IP));
        assertNotNull(server.getAttributes().get(NET_PEER_PORT));
        assertEquals("ip_tcp", server.getAttributes().get(NET_TRANSPORT));

        SpanData client = spans.get(1);
        assertEquals("helloworld.Greeter/SayHello", client.getName());
        assertEquals(SpanKind.CLIENT, client.getKind());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", client.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", client.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), client.getAttributes().get(RPC_GRPC_STATUS_CODE));

        assertEquals(server.getTraceId(), client.getTraceId());
    }

    @Test
    void error() {
        try {
            greeterStub.sayHello(HelloRequest.newBuilder().setName("error").build())
                    .map(HelloReply::getMessage)
                    .await()
                    .atMost(Duration.ofSeconds(5));
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals(2, spans.size());

        SpanData server = spans.get(0);
        assertEquals("helloworld.Greeter/SayHello", server.getName());
        assertEquals(SpanKind.SERVER, server.getKind());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", server.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.UNKNOWN.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(NET_PEER_IP));
        assertNotNull(server.getAttributes().get(NET_PEER_PORT));
        assertEquals("ip_tcp", server.getAttributes().get(NET_TRANSPORT));

        SpanData client = spans.get(1);
        assertEquals("helloworld.Greeter/SayHello", client.getName());
        assertEquals(SpanKind.CLIENT, client.getKind());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", client.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", client.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.UNKNOWN.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));

        assertEquals(server.getTraceId(), client.getTraceId());
    }

    @Test
    void withCdi() {
        assertEquals("Hello Naruto", helloBean.hello("Naruto"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        assertEquals(3, spans.size());

        assertEquals(spans.get(0).getTraceId(), spans.get(1).getTraceId());
        assertEquals(spans.get(0).getTraceId(), spans.get(2).getTraceId());
    }

    @GrpcService
    public static class HelloService extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(final HelloRequest request, final StreamObserver<HelloReply> responseObserver) {
            if (request.getName().equals("error")) {
                responseObserver.onError(new RuntimeException());
                return;
            }

            responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
            responseObserver.onCompleted();
        }
    }

    @ApplicationScoped
    public static class HelloBean {
        @GrpcClient
        Greeter greeter;

        // TODO - radcortez - how to propagate the context if this is a Uni?
        @WithSpan
        public String hello(String name) {
            return greeter.sayHello(HelloRequest.newBuilder().setName(name).build())
                    .onItem()
                    .transform(HelloReply::getMessage)
                    .await()
                    .atMost(Duration.ofSeconds(5));
        }
    }

    @GrpcClient
    MutinyStreamingGrpc.MutinyStreamingStub streamingStub;

    @Test
    void stream() {
        Multi<Item> request = Multi.createFrom().items(item("Goku"), item("Vegeta"), item("Piccolo"), item("Beerus"),
                item("Whis"));
        Multi<Item> response = streamingStub.pipe(request);

        List<String> items = response.map(Item::getMessage).collect().asList().await().atMost(Duration.ofSeconds(5));
        assertTrue(items.contains("Hello Goku"));
        assertTrue(items.contains("Hello Vegeta"));
        assertTrue(items.contains("Hello Piccolo"));
        assertTrue(items.contains("Hello Beerus"));
        assertTrue(items.contains("Hello Whis"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals(2, spans.size());

        SpanData server = spans.get(0);
        assertEquals("streaming.Streaming/Pipe", server.getName());
        assertEquals(SpanKind.SERVER, server.getKind());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", server.getAttributes().get(RPC_SERVICE));
        assertEquals("Pipe", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(NET_PEER_IP));
        assertNotNull(server.getAttributes().get(NET_PEER_PORT));
        assertEquals("ip_tcp", server.getAttributes().get(NET_TRANSPORT));

        SpanData client = spans.get(1);
        assertEquals("streaming.Streaming/Pipe", client.getName());
        assertEquals(SpanKind.CLIENT, client.getKind());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", client.getAttributes().get(RPC_SERVICE));
        assertEquals("Pipe", client.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), client.getAttributes().get(RPC_GRPC_STATUS_CODE));

        assertEquals(server.getTraceId(), client.getTraceId());
    }

    @GrpcService
    public static class StreamService implements Streaming {
        @Override
        public Multi<Item> pipe(final Multi<Item> request) {
            return request.onItem().transform(item -> item("Hello " + item.getMessage()));
        }
    }

    private static Item item(final String message) {
        return Item.newBuilder().setMessage(message).build();
    }
}
