package io.quarkus.grpc.example.multiplex;

import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.Multiplex;
import io.grpc.examples.multiplex.StringRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.semconv.ResourceAttributes;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SuppressWarnings("NewClassNamingConvention")
public class BaseTest {
    @GrpcClient("streaming")
    Multiplex multiplex;

    Traces traces;

    @BeforeEach
    @AfterEach
    void setUp() {
        traces.reset();
    }

    @Test
    @Timeout(10)
    public void testParse() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 10)
                .map(x -> StringRequest.newBuilder()
                        // fixme a span per StringRequest
                        .setNumber(x.toString())
                        .build());

        AssertSubscriber<LongReply> subscriber = multiplex.parse(multi)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        Set<Long> longSet = subscriber.awaitCompletion()
                .getItems()
                .stream()
                .map(LongReply::getValue)
                .collect(Collectors.toSet());

        Set<Long> expected = LongStream.range(1, 10).boxed().collect(Collectors.toSet());
        Assertions.assertEquals(expected, longSet);
        verifyTraces();
    }

    private void verifyTraces() {
        await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(traces.getTraceRequests()).hasSize(1));

        ExportTraceServiceRequest request = traces.getTraceRequests().get(0);
        assertThat(request.getResourceSpansCount()).isEqualTo(1);

        ResourceSpans resourceSpans = request.getResourceSpans(0);
        assertThat(resourceSpans.getResource().getAttributesList())
                .contains(
                        KeyValue.newBuilder()
                                .setKey(ResourceAttributes.WEBENGINE_NAME.getKey())
                                .setValue(AnyValue.newBuilder()
                                        .setStringValue("Quarkus").build())
                                .build());
        assertThat(resourceSpans.getScopeSpansCount()).isEqualTo(1);
        ScopeSpans scopeSpans = resourceSpans.getScopeSpans(0);
        assertThat(scopeSpans.getSpansCount()).isEqualTo(2);
    }
}
