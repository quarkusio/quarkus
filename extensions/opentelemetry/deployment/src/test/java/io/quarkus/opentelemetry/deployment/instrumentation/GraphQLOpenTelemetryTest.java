package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_STATUS_CODE;
import static io.quarkus.opentelemetry.deployment.common.SemconvResolver.assertSemanticAttribute;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.SemconvResolver;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GraphQLOpenTelemetryTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, CustomCDIBean.class, TestSpanExporterProvider.class,
                            TestSpanExporter.class, SemconvResolver.class)
                    .addAsResource(new StringAsset("smallrye.graphql.allowGet=true"), "application.properties")
                    .addAsResource(new StringAsset("smallrye.graphql.printDataFetcherException=true"), "application.properties")
                    .addAsResource(new StringAsset("smallrye.graphql.events.enabled=true"), "application.properties")
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void singleResultQueryTraceTest() {
        String request = getPayload("query hello {\n" +
                "  hello\n" +
                "}",
                null);

        assertSuccessfulRequestContainingMessages(request, "hello xyz");

        spanExporter.assertSpanCount(3);

        final List<SpanData> spans = Collections.unmodifiableList(spanExporter.getFinishedSpanItems(3));
        assertTimeForSpans(spans);

        final SpanData httpSpan = assertHttpSpan(spans);

        final SpanData operationSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, httpSpan.getSpanId());
        assertGraphQLSpan(operationSpan, "GraphQL:hello", "QUERY", "hello");

        final SpanData querySpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, operationSpan.getSpanId());
        assertEquals("HelloResource.hello", querySpan.getName());
    }

    @Test
    void multipleResultQueryTraceTest() {
        String request = getPayload("query {\n" +
                "  hellos\n" +
                "}",
                null);

        assertSuccessfulRequestContainingMessages(request, "[\"hello, hi, hey\"]");

        spanExporter.assertSpanCount(3);

        final List<SpanData> spans = Collections.unmodifiableList(spanExporter.getFinishedSpanItems(3));
        assertTimeForSpans(spans);
        final SpanData httpSpan = assertHttpSpan(spans);

        final SpanData operationSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, httpSpan.getSpanId());
        assertGraphQLSpan(operationSpan, "GraphQL", "QUERY", "");

        final SpanData querySpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, operationSpan.getSpanId());
        assertEquals("HelloResource.hellos", querySpan.getName());
    }

    @Test
    @Disabled // TODO: flaky test, find out how to fix it
    void nestedCdiBeanInsideQueryTraceTest() throws ExecutionException, InterruptedException {
        String request = getPayload("query {\n" +
                "  helloAfterSecond\n" +
                "}",
                null);
        int iterations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(50);

        try {
            CompletableFuture<Void>[] futures = new CompletableFuture[iterations];
            for (int i = 0; i < iterations; i++) {
                futures[i] = CompletableFuture.supplyAsync(() -> assertSuccessfulRequestContainingMessages(request, "hello"),
                        executor);
            }
            getNestedCdiBeanTestResult(futures, iterations);
        } finally {
            executor.shutdown();
        }
    }

    private void getNestedCdiBeanTestResult(CompletableFuture<Void>[] futures, int iterations)
            throws InterruptedException, ExecutionException {
        CompletableFuture.allOf(futures).get();
        int numberOfSpansPerGroup = 4;
        spanExporter.assertSpanCount(iterations * numberOfSpansPerGroup);
        final List<SpanData> spans = Collections
                .unmodifiableList(spanExporter.getFinishedSpanItems(iterations * numberOfSpansPerGroup));

        List<List<SpanData>> spanCollections = spans.stream()
                .collect(Collectors.groupingBy(
                        SpanData::getTraceId,
                        LinkedHashMap::new, // use a LinkedHashMap to preserve insertion order
                        Collectors.toList()))
                .values().stream()
                .collect(Collectors.toList());

        assertEquals(spanCollections.size(), iterations);
        for (List<SpanData> spanGroup : spanCollections) {
            assertTimeForSpans(spanGroup);
            final SpanData httpSpan = assertHttpSpan(spanGroup);

            final SpanData operationSpan = getSpanByKindAndParentId(spanGroup, SpanKind.INTERNAL, httpSpan.getSpanId());
            assertGraphQLSpan(operationSpan, "GraphQL", "QUERY", "");

            final SpanData querySpan = getSpanByKindAndParentId(spanGroup, SpanKind.INTERNAL, operationSpan.getSpanId());
            assertEquals(operationSpan.getSpanId(), querySpan.getParentSpanId());
            assertEquals("HelloResource.helloAfterSecond", querySpan.getName());

            final SpanData cdiBeanSpan = getSpanByKindAndParentId(spanGroup, SpanKind.INTERNAL, querySpan.getSpanId());
            assertEquals(querySpan.getSpanId(), cdiBeanSpan.getParentSpanId());
            assertEquals("CustomCDIBean.waitForSomeTime", cdiBeanSpan.getName());
        }
    }

    @Test
    void mutationTraceTest() {
        String request = getPayload("mutation addHello { createHello }", null);

        assertSuccessfulRequestContainingMessages(request, "hello created");

        spanExporter.assertSpanCount(3);

        final List<SpanData> spans = Collections.unmodifiableList(spanExporter.getFinishedSpanItems(3));
        assertTimeForSpans(spans);
        final SpanData httpSpan = assertHttpSpan(spans);

        final SpanData operationSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, httpSpan.getSpanId());
        assertGraphQLSpan(operationSpan, "GraphQL:addHello", "MUTATION", "addHello");

        final SpanData mutationSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, operationSpan.getSpanId());
        assertEquals("HelloResource.createHello", mutationSpan.getName());
    }

    @GraphQLApi
    public static class HelloResource {
        @Inject
        CustomCDIBean cdiBean;

        @WithSpan
        @Query
        public String hello() {
            return "hello xyz";
        }

        @WithSpan
        @Query
        public List<String> hellos() {
            return List.of("hello, hi, hey");
        }

        @WithSpan
        @Query
        public String helloAfterSecond() throws InterruptedException {
            cdiBean.waitForSomeTime();
            return "hello";
        }

        // FIXME: error handling in spans
        @WithSpan
        @Query
        public String errorHello() {
            throw new RuntimeException("Error");
        }

        @WithSpan
        @Mutation
        public String createHello() {
            return "hello created";
        }

        // TODO: SOURCE field tests (sync)
        // TODO: SOURCE field tests (async)
        // TODO: nonblocking queries/mutations (reactive) tests
        // TODO: subscriptions tests (?)

    }

    @ApplicationScoped
    public static class CustomCDIBean {
        @WithSpan
        public void waitForSomeTime() throws InterruptedException {
            Thread.sleep(10);
        }
    }

    private Void assertSuccessfulRequestContainingMessages(String request, String... messages) {
        org.hamcrest.Matcher messageMatcher = Arrays.stream(messages)
                .map(CoreMatchers::containsString)
                .reduce(Matchers.allOf(), (a, b) -> Matchers.allOf(a, b));

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(messageMatcher);
        return null;
    }

    private void assertTimeForSpans(List<SpanData> spans) {
        // Method expects for each span to have at least one child span
        if (spans.size() <= 1) {
            return;
        }

        SpanData current = getSpanByKindAndParentId(spans, SpanKind.SERVER, "0000000000000000");
        for (int i = 0; i < spans.size() - 1; i++) {
            SpanData successor = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, current.getSpanId());
            assertTrue(current.getStartEpochNanos() <= successor.getStartEpochNanos());
            assertTrue(current.getEndEpochNanos() >= successor.getEndEpochNanos());
            current = successor;
        }
    }

    private SpanData assertHttpSpan(List<SpanData> spans) {
        final SpanData server = getSpanByKindAndParentId(spans, SpanKind.SERVER, "0000000000000000");
        assertEquals("POST /graphql", server.getName());
        assertSemanticAttribute(server, (long) HTTP_OK, HTTP_STATUS_CODE);
        assertSemanticAttribute(server, "POST", HTTP_METHOD);
        assertEquals("/graphql", server.getAttributes().get(HTTP_ROUTE));
        return server;
    }

    private void assertGraphQLSpan(SpanData span, String name, String OperationType, String OperationName) {
        assertEquals(name, span.getName());
        assertNotNull(span.getAttributes().get(stringKey("graphql.executionId")));
        assertEquals(OperationType, span.getAttributes().get(stringKey("graphql.operationType")));
        assertEquals(OperationName, span.getAttributes().get(stringKey("graphql.operationName")));
    }

    private String getPayload(String query, String variables) {
        JsonObject jsonObject = createRequestBody(query, variables);
        return jsonObject.toString();
    }

    private JsonObject createRequestBody(String graphQL, String variables) {
        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonObject vjo = Json.createObjectBuilder().build();

        // Extract the operation name from the GraphQL query
        String operationName = null;
        if (graphQL != null && !graphQL.isEmpty()) {
            Matcher matcher = Pattern.compile("(query|mutation|subscription)\\s+([\\w-]+)?").matcher(graphQL);
            if (matcher.find()) {
                operationName = matcher.group(2);
                job.add(QUERY, graphQL);
            }
        }

        // Parse variables if present
        if (variables != null && !variables.isEmpty()) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(variables))) {
                vjo = jsonReader.readObject();
            }
        }

        if (operationName != null) {
            job.add(OPERATION_NAME, operationName);
        }

        return job.add(VARIABLES, vjo).build();
    }

    private static final String MEDIATYPE_JSON = "application/json";
    private static final String QUERY = "query";
    private static final String VARIABLES = "variables";
    private static final String OPERATION_NAME = "operationName";

}
