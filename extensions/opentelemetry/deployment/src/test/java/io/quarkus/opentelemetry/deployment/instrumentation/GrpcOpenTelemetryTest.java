package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_GRPC_STATUS_CODE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SYSTEM;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.opentelemetry.deployment.Greeter;
import io.quarkus.opentelemetry.deployment.GreeterBean;
import io.quarkus.opentelemetry.deployment.GreeterClient;
import io.quarkus.opentelemetry.deployment.GreeterGrpc;
import io.quarkus.opentelemetry.deployment.HelloProto;
import io.quarkus.opentelemetry.deployment.HelloReply;
import io.quarkus.opentelemetry.deployment.HelloReplyOrBuilder;
import io.quarkus.opentelemetry.deployment.HelloRequest;
import io.quarkus.opentelemetry.deployment.HelloRequestOrBuilder;
import io.quarkus.opentelemetry.deployment.Item;
import io.quarkus.opentelemetry.deployment.ItemOrBuilder;
import io.quarkus.opentelemetry.deployment.MutinyGreeterGrpc;
import io.quarkus.opentelemetry.deployment.MutinyGreeterGrpc.MutinyGreeterStub;
import io.quarkus.opentelemetry.deployment.MutinyStreamingGrpc;
import io.quarkus.opentelemetry.deployment.MutinyStreamingGrpc.MutinyStreamingStub;
import io.quarkus.opentelemetry.deployment.Streaming;
import io.quarkus.opentelemetry.deployment.StreamingBean;
import io.quarkus.opentelemetry.deployment.StreamingClient;
import io.quarkus.opentelemetry.deployment.StreamingGrpc;
import io.quarkus.opentelemetry.deployment.StreamingProto;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;

public class GrpcOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(TestSpanExporter.class.getPackage())
                    .addClasses(HelloService.class)
                    .addClasses(GreeterGrpc.class, MutinyGreeterGrpc.class,
                            Greeter.class, GreeterBean.class, GreeterClient.class,
                            HelloProto.class, HelloRequest.class, HelloRequestOrBuilder.class,
                            HelloReply.class, HelloReplyOrBuilder.class)
                    .addClasses(StreamingService.class)
                    .addClasses(StreamingGrpc.class, MutinyStreamingGrpc.class,
                            Streaming.class, StreamingBean.class, StreamingClient.class,
                            StreamingProto.class, Item.class, ItemOrBuilder.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                    .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider"))
            .withConfigurationResource("application-default.properties")
            .overrideConfigKey("quarkus.grpc.clients.greeter.host", "localhost")
            .overrideConfigKey("quarkus.grpc.clients.greeter.port", "9001")
            .overrideConfigKey("quarkus.grpc.clients.streaming.host", "localhost")
            .overrideConfigKey("quarkus.grpc.clients.streaming.port", "9001");

    @Inject
    TestSpanExporter spanExporter;
    @GrpcClient("greeter")
    MutinyGreeterStub greeterStub;
    @Inject
    HelloBean helloBean;
    @GrpcClient("streaming")
    MutinyStreamingStub streamingStub;

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

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        assertEquals(3, spans.size());

        final SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals("helloworld.Greeter/SayHello", client.getName());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", client.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", client.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), client.getAttributes().get(RPC_GRPC_STATUS_CODE));

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals("helloworld.Greeter/SayHello", server.getName());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", server.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(SERVER_PORT));
        assertNotNull(server.getAttributes().get(SERVER_ADDRESS));

        final SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("span.internal", internal.getName());
        assertEquals("value", internal.getAttributes().get(stringKey("grpc.internal")));

        assertEquals(internal.getTraceId(), server.getTraceId());
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

        final SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals("helloworld.Greeter/SayHello", client.getName());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", client.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", client.getAttributes().get(RPC_METHOD));

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals("helloworld.Greeter/SayHello", server.getName());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", server.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.UNKNOWN.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(SERVER_PORT));
        assertNotNull(server.getAttributes().get(SERVER_ADDRESS));
        assertEquals(Status.Code.UNKNOWN.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));

        assertEquals(server.getTraceId(), client.getTraceId());
    }

    @Test
    void withCdi() {
        assertEquals("Hello Naruto", helloBean.hello("Naruto"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(4);
        assertEquals(4, spans.size());

        final SpanData first = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        final SpanData second = getSpanByKindAndParentId(spans, CLIENT, first.getSpanId());
        final SpanData third = getSpanByKindAndParentId(spans, SERVER, second.getSpanId());
        final SpanData fourth = getSpanByKindAndParentId(spans, INTERNAL, third.getSpanId());

        assertThat(first.getTraceId()).isIn(second.getTraceId(), third.getTraceId(), fourth.getTraceId());
    }

    @Test
    void streaming() {
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

        final SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals("streaming.Streaming/Pipe", client.getName());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", client.getAttributes().get(RPC_SERVICE));
        assertEquals("Pipe", client.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), client.getAttributes().get(RPC_GRPC_STATUS_CODE));

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals("streaming.Streaming/Pipe", server.getName());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", server.getAttributes().get(RPC_SERVICE));
        assertEquals("Pipe", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(SERVER_PORT));
        assertNotNull(server.getAttributes().get(SERVER_ADDRESS));
        assertEquals("true", server.getAttributes().get(stringKey("grpc.service.propagated")));

        assertEquals(server.getTraceId(), client.getTraceId());
    }

    @Test
    void streamingBlocking() {
        Multi<Item> request = Multi.createFrom().items(item("Goku"), item("Vegeta"), item("Piccolo"), item("Beerus"),
                item("Whis"));
        Multi<Item> response = streamingStub.pipeBlocking(request);

        List<String> items = response.map(Item::getMessage).collect().asList().await().atMost(Duration.ofSeconds(5));
        assertTrue(items.contains("Hello Goku"));
        assertTrue(items.contains("Hello Vegeta"));
        assertTrue(items.contains("Hello Piccolo"));
        assertTrue(items.contains("Hello Beerus"));
        assertTrue(items.contains("Hello Whis"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals(2, spans.size());

        final SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals("streaming.Streaming/PipeBlocking", client.getName());
        assertEquals("grpc", client.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", client.getAttributes().get(RPC_SERVICE));
        assertEquals("PipeBlocking", client.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), client.getAttributes().get(RPC_GRPC_STATUS_CODE));

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals("streaming.Streaming/PipeBlocking", server.getName());
        assertEquals("grpc", server.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", server.getAttributes().get(RPC_SERVICE));
        assertEquals("PipeBlocking", server.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), server.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(server.getAttributes().get(SERVER_PORT));
        assertNotNull(server.getAttributes().get(SERVER_ADDRESS));
        assertEquals("true", server.getAttributes().get(stringKey("grpc.service.propagated.blocking")));

        assertEquals(server.getTraceId(), client.getTraceId());
    }

    @GrpcService
    public static class HelloService extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(final HelloRequest request, final StreamObserver<HelloReply> responseObserver) {
            if (request.getName().equals("error")) {
                responseObserver.onError(new RuntimeException());
                return;
            }

            Tracer tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
            Span span = tracer.spanBuilder("span.internal")
                    .setSpanKind(INTERNAL)
                    .setAttribute("grpc.internal", "value")
                    .startSpan();
            span.end();

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

    @GrpcService
    public static class StreamingService implements Streaming {
        @Override
        public Multi<Item> pipe(final Multi<Item> request) {
            Span.current().setAttribute("grpc.service.propagated", "true");
            return request.onItem().transform(item -> item("Hello " + item.getMessage()));
        }

        @Blocking
        @Override
        public Multi<Item> pipeBlocking(final Multi<Item> request) {
            Span.current().setAttribute("grpc.service.propagated.blocking", "true");
            return request.onItem().transform(item -> item("Hello " + item.getMessage()));
        }
    }

    private static Item item(final String message) {
        return Item.newBuilder().setMessage(message).build();
    }
}
