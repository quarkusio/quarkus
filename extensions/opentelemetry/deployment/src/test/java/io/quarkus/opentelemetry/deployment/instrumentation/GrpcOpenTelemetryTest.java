package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
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
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;

public class GrpcOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
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
            .overrideConfigKey("quarkus.grpc.clients.greeter.port", "8081")
            .overrideConfigKey("quarkus.grpc.clients.streaming.host", "localhost")
            .overrideConfigKey("quarkus.grpc.clients.streaming.port", "8081");

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
    @RunOnVertxContext(duplicateContext = true, runOnEventLoop = false)
    void grpc() {
        String response = greeterStub.sayHello(
                HelloRequest.newBuilder().setName("Naruto").build())
                .map(HelloReply::getMessage)
                .await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello Naruto", response);

        // HTTP spans are suppressed for gRPC — only gRPC CLIENT, gRPC SERVER, and internal spans remain
        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        assertEquals(3, spans.size());

        final SpanData grpcClient = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000",
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("helloworld.Greeter/SayHello", grpcClient.getName());
        assertEquals("grpc", grpcClient.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", grpcClient.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", grpcClient.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), grpcClient.getAttributes().get(RPC_GRPC_STATUS_CODE));

        final SpanData grpcServer = getSpanByKindAndParentId(spans, SERVER, grpcClient.getSpanId(),
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("helloworld.Greeter/SayHello", grpcServer.getName());
        assertEquals("grpc", grpcServer.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", grpcServer.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", grpcServer.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), grpcServer.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertNotNull(grpcServer.getAttributes().get(NETWORK_PEER_PORT));
        assertNotNull(grpcServer.getAttributes().get(NETWORK_PEER_ADDRESS));

        final SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, grpcServer.getSpanId());
        assertEquals("span.internal", internal.getName());
        assertEquals("value", internal.getAttributes().get(stringKey("grpc.internal")));

        assertEquals(grpcClient.getTraceId(), grpcServer.getTraceId());
        assertEquals(grpcServer.getTraceId(), internal.getTraceId());
    }

    @Test
    @RunOnVertxContext(duplicateContext = true, runOnEventLoop = false)
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

        final SpanData grpcClient = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000",
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("helloworld.Greeter/SayHello", grpcClient.getName());
        assertEquals("grpc", grpcClient.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", grpcClient.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", grpcClient.getAttributes().get(RPC_METHOD));

        final SpanData grpcServer = getSpanByKindAndParentId(spans, SERVER, grpcClient.getSpanId(),
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("helloworld.Greeter/SayHello", grpcServer.getName());
        assertEquals("grpc", grpcServer.getAttributes().get(RPC_SYSTEM));
        assertEquals("helloworld.Greeter", grpcServer.getAttributes().get(RPC_SERVICE));
        assertEquals("SayHello", grpcServer.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.UNKNOWN.value(), grpcServer.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertEquals(grpcClient.getTraceId(), grpcServer.getTraceId());
    }

    @Test
    @RunOnVertxContext(duplicateContext = true, runOnEventLoop = false)
    void withCdi() {
        assertEquals("Hello Naruto", helloBean.hello("Naruto"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(4);
        assertEquals(4, spans.size());

        final SpanData first = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        final SpanData grpcClient = getSpanByKindAndParentId(spans, CLIENT, first.getSpanId(),
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        final SpanData grpcServer = getSpanByKindAndParentId(spans, SERVER, grpcClient.getSpanId(),
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        final SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, grpcServer.getSpanId());

        assertThat(first.getTraceId()).isIn(grpcClient.getTraceId(),
                grpcServer.getTraceId(), internal.getTraceId());
    }

    @Test
    @RunOnVertxContext(duplicateContext = true, runOnEventLoop = false)
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

        final SpanData grpcClient = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000",
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("streaming.Streaming/Pipe", grpcClient.getName());
        assertEquals("grpc", grpcClient.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", grpcClient.getAttributes().get(RPC_SERVICE));
        assertEquals("Pipe", grpcClient.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), grpcClient.getAttributes().get(RPC_GRPC_STATUS_CODE));

        final SpanData grpcServer = getSpanByKindAndParentId(spans, SERVER, grpcClient.getSpanId(),
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("streaming.Streaming/Pipe", grpcServer.getName());
        assertEquals("grpc", grpcServer.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", grpcServer.getAttributes().get(RPC_SERVICE));
        assertEquals("Pipe", grpcServer.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), grpcServer.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertEquals("true", grpcServer.getAttributes().get(stringKey("grpc.service.propagated")));

        assertEquals(grpcClient.getTraceId(), grpcServer.getTraceId());
    }

    @Test
    @RunOnVertxContext(duplicateContext = true, runOnEventLoop = false)
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

        final SpanData grpcClient = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000",
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("streaming.Streaming/PipeBlocking", grpcClient.getName());
        assertEquals("grpc", grpcClient.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", grpcClient.getAttributes().get(RPC_SERVICE));
        assertEquals("PipeBlocking", grpcClient.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), grpcClient.getAttributes().get(RPC_GRPC_STATUS_CODE));

        final SpanData grpcServer = getSpanByKindAndParentId(spans, SERVER, grpcClient.getSpanId(),
                span -> "grpc".equals(span.getAttributes().get(RPC_SYSTEM)));
        assertEquals("streaming.Streaming/PipeBlocking", grpcServer.getName());
        assertEquals("grpc", grpcServer.getAttributes().get(RPC_SYSTEM));
        assertEquals("streaming.Streaming", grpcServer.getAttributes().get(RPC_SERVICE));
        assertEquals("PipeBlocking", grpcServer.getAttributes().get(RPC_METHOD));
        assertEquals(Status.Code.OK.value(), grpcServer.getAttributes().get(RPC_GRPC_STATUS_CODE));
        assertEquals("true", grpcServer.getAttributes().get(stringKey("grpc.service.propagated.blocking")));

        assertEquals(grpcClient.getTraceId(), grpcServer.getTraceId());
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
