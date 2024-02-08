package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.it.opentelemetry.util.EndUserResource;
import io.quarkus.opentelemetry.runtime.exporter.otlp.EndUserSpanProcessor;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.security.TestSecurity;

@TestHTTPEndpoint(EndUserResource.class)
@TestSecurity(user = "testUser", roles = { "admin", "user" })
public abstract class AbstractEndUserTest {

    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Inject
    Instance<EndUserSpanProcessor> endUserSpanProcessor;

    protected final Predicate<Instance<EndUserSpanProcessor>> injectionPredicate;

    public AbstractEndUserTest(Predicate<Instance<EndUserSpanProcessor>> predicate) {
        this.injectionPredicate = predicate;
    }

    @BeforeEach
    @AfterEach
    protected void reset() {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            // make sure spans from previous tests are not included
            List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
            if (finishedSpanItems.size() > 0) {
                inMemorySpanExporter.reset();
            }
            return finishedSpanItems.size() == 0;
        });
    }

    protected List<SpanData> getSpansName(final String spanName) {
        return inMemorySpanExporter.getFinishedSpanItems().stream()
                .filter(span -> spanName.equals(span.getName()))
                .collect(Collectors.toList());
    }

    protected abstract void evaluateAttributes(Attributes attributes);

    @Test
    protected void baseTest() {
        assertTrue(this.injectionPredicate.test(endUserSpanProcessor));

        given()
                .when().get()
                .then()
                .statusCode(200);

        final String spanName = "GET /otel/enduser";
        await().atMost(5, SECONDS).until(() -> getSpansName(spanName).size() > 0);
        Optional<SpanData> spanData = getSpansName(spanName).stream().findFirst();

        assertTrue(spanData.isPresent(), "Span not found for " + spanName);
        evaluateAttributes(spanData.get().getAttributes());
    }

}
