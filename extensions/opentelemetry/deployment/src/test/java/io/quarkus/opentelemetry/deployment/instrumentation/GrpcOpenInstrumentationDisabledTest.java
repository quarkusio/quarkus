package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
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
import io.quarkus.opentelemetry.deployment.MutinyGreeterGrpc;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class GrpcOpenInstrumentationDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addClasses(HelloService.class)
                    .addClasses(GreeterGrpc.class, MutinyGreeterGrpc.class,
                            Greeter.class, GreeterBean.class, GreeterClient.class,
                            HelloProto.class, HelloRequest.class, HelloRequestOrBuilder.class,
                            HelloReply.class, HelloReplyOrBuilder.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("application-default.properties")
            .overrideConfigKey("quarkus.grpc.clients.hello.host", "localhost")
            .overrideConfigKey("quarkus.grpc.clients.hello.port", "9001")
            .overrideConfigKey("quarkus.otel.instrument.grpc", "false");

    @Inject
    TestSpanExporter spanExporter;

    @GrpcClient
    Greeter hello;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void testTratestTracingDisabled() {
        String response = hello.sayHello(
                HelloRequest.newBuilder().setName("ping").build())
                .map(HelloReply::getMessage)
                .await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello ping", response);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spans.size());

        SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertEquals("span.internal", internal.getName());
        assertEquals("value", internal.getAttributes().get(stringKey("grpc.internal")));
    }

    @GrpcService
    public static class HelloService implements Greeter {

        @Inject
        Tracer tracer;

        @Override
        public Uni<HelloReply> sayHello(HelloRequest request) {
            Span span = tracer.spanBuilder("span.internal")
                    .setSpanKind(INTERNAL)
                    .setAttribute("grpc.internal", "value")
                    .startSpan();
            span.end();
            return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        }
    }

}
