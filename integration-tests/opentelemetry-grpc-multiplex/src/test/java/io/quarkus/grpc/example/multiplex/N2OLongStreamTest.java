package io.quarkus.grpc.example.multiplex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.opentelemetry.proto.trace.v1.Span;
import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.collector.OtelCollectorLifecycleManager;

@QuarkusTest
@TestProfile(N2OGRPCTestProfile.class)
@QuarkusTestResource(value = OtelCollectorLifecycleManager.class, restrictToAnnotatedClass = true)
public class N2OLongStreamTest extends BaseTest {
    @Override
    void verifySpans(List<Span> scopeSpans) {
        //2 per number + 1 client, 1 server
        assertThat(scopeSpans).hasSize(9);
        assertAndGet(scopeSpans,
                null, "streaming.Multiplex/Parse", Span.SpanKind.SPAN_KIND_CLIENT,
                Map.of("rpc.system", "grpc"));
        Span clientSpan = assertAndGet(scopeSpans,
                null, "POST", Span.SpanKind.SPAN_KIND_CLIENT,
                Collections.emptyMap());
        Span serverSpan = assertAndGet(scopeSpans,
                clientSpan.getSpanId(), "streaming.Multiplex/Parse", Span.SpanKind.SPAN_KIND_SERVER,
                Map.of("rpc.system", "grpc"));
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
