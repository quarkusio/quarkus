package io.quarkus.grpc.example.multiplex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import io.opentelemetry.proto.trace.v1.Span;
import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.collector.OtelCollectorLifecycleManager;

@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class O2NLongStreamTest extends BaseTest {
    @Override
    void verifySpans(List<Span> scopeSpans) {
        //2 per number + 1 client, 1 server
        assertThat(scopeSpans).hasSize(8);
        Span clientSpan = assertAndGet(scopeSpans,
                null, "streaming.Multiplex/Parse", Span.SpanKind.SPAN_KIND_CLIENT,
                Map.of("rpc.system", "grpc"));
        Span serverSpan = assertAndGet(scopeSpans,
                clientSpan.getSpanId(), "POST", Span.SpanKind.SPAN_KIND_SERVER,
                Map.of("url.scheme", "http"));
        Span child = assertAndGet(scopeSpans,
                serverSpan.getSpanId(), "child", Span.SpanKind.SPAN_KIND_INTERNAL,
                Map.of("child.number", "1"));
        assertAndGet(scopeSpans,
                child.getSpanId(), "TheService.processResult", Span.SpanKind.SPAN_KIND_INTERNAL,
                Map.of("inner.number", "1"));
        child = assertAndGet(scopeSpans,
                serverSpan.getSpanId(), "child", Span.SpanKind.SPAN_KIND_INTERNAL,
                Map.of("child.number", "2"));
        assertAndGet(scopeSpans,
                child.getSpanId(), "TheService.processResult", Span.SpanKind.SPAN_KIND_INTERNAL,
                Map.of("inner.number", "2"));
        child = assertAndGet(scopeSpans,
                serverSpan.getSpanId(), "child", Span.SpanKind.SPAN_KIND_INTERNAL,
                Map.of("child.number", "3"));
        assertAndGet(scopeSpans,
                child.getSpanId(), "TheService.processResult", Span.SpanKind.SPAN_KIND_INTERNAL,
                Map.of("inner.number", "3"));
    }
}
