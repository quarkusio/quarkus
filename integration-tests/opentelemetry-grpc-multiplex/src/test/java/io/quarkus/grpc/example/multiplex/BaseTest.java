package io.quarkus.grpc.example.multiplex;

import com.google.protobuf.ByteString;
import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.Multiplex;
import io.grpc.examples.multiplex.StringRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.semconv.ResourceAttributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.opentelemetry.collector.Traces;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("NewClassNamingConvention")
public abstract class BaseTest {
    @GrpcClient("streaming")
    Multiplex multiplex;

    Traces traces;

    static Span assertAndGet(List<Span> scopeSpans,
                             ByteString parent,
                             String spanName,
                             Span.SpanKind kind,
                             Map<String, String> keyValues) {
        List<Span> matchingSpans = scopeSpans.stream()
                .filter(span -> span.getName().equals(spanName))
                .filter(span -> span.getKind().equals(kind))
                .filter(span -> keyValues.entrySet().stream()
                        .allMatch(entry -> span.getAttributesList().stream()
                                .filter(attr -> attr.getKey().equals(entry.getKey()))
                                .anyMatch(attr -> attr.getValue().getStringValue().equals(entry.getValue()))))
                .collect(Collectors.toList());

        if (matchingSpans.isEmpty()) {
            fail("Span not found: " + spanName);
        }
        if (matchingSpans.size() > 1) {
            fail("Multiple spans found with name: " + spanName);
        }

        assertThat(toHex(matchingSpans.get(0).getParentSpanId())).isEqualTo(toHex(parent));

        return matchingSpans.get(0);
    }

    static String toHex(ByteString value) {
        if (value == null) {
            return "";
        }
        byte[] byteArray = value.toByteArray();
        return "" + IntStream.range(0, byteArray.length)
                .mapToObj(i -> String.format("%02X", byteArray[i]))
                .collect(Collectors.joining(""));
    }

    @BeforeEach
    @AfterEach
    void setUp() {
        traces.reset();
    }

    @Test
    @Timeout(60)
    public void testParse() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 4)
                .map(x -> StringRequest.newBuilder()
                        // fixme a span per StringRequest
                        .setNumber(x.toString())
                        .build());

        AssertSubscriber<LongReply> subscriber = multiplex.parse(multi)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        Set<Long> longSet = subscriber.awaitCompletion(Duration.ofSeconds(30))
                .getItems()
                .stream()
                .map(LongReply::getValue)
                .collect(Collectors.toSet());

        Set<Long> expected = LongStream.range(1, 4).boxed().collect(Collectors.toSet());
        Assertions.assertEquals(expected, longSet);
        System.out.println("longSet size " + longSet.size());

        // verify traces
        await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(traces.getTraceRequests()).hasSizeGreaterThan(1));

        List<ExportTraceServiceRequest> traceRequests = traces.getTraceRequests();
        List<ResourceSpans> resourceSpans = traces.getTraceRequests().stream()
                .map(ExportTraceServiceRequest::getResourceSpansList)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        resourceSpans.stream().forEach(rs -> {
            assertThat(rs.getResource().getAttributesList())
                    .contains(
                            KeyValue.newBuilder()
                                    .setKey(ResourceAttributes.WEBENGINE_NAME.getKey())
                                    .setValue(AnyValue.newBuilder()
                                            .setStringValue("Quarkus").build())
                                    .build());
        });

        List<ScopeSpans> scopeSpans = resourceSpans.stream()
                .map(rs -> rs.getScopeSpansList())
                .flatMap(List::stream)
                .collect(Collectors.toList());

//        assertThat(resourceSpans.getScopeSpansCount()).isEqualTo(1);
//        ScopeSpans scopeSpans = resourceSpans.getScopeSpans(0);

        System.out.println(scopeSpans.stream().
                map(ss -> ss.getSpansList())
                .flatMap(List::stream)
                .map(span -> toHex(span.getParentSpanId()) + "::" +
                        toHex(span.getSpanId()) + "::" +
                        span.getName() + "::" +
                        span.getKind() + "::\n" +
                        span.getAttributesList().stream()
                                .map(attrib -> "   " + attrib.getKey() + ":" + attrib.getValue())
                                .collect(Collectors.joining(" ")) + "\n")
                .collect(Collectors.toList()));

        verifySpans(scopeSpans.stream().
                map(ss -> ss.getSpansList())
                .flatMap(List::stream)
                .collect(Collectors.toList()));

//        assertThat(scopeSpans.getSpansCount()).isEqualTo(8);
    }

    abstract void verifySpans(List<Span> scopeSpans);
}
