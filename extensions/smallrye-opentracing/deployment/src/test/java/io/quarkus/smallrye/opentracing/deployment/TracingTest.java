package io.quarkus.smallrye.opentracing.deployment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class TracingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TestResource.class)
                    .addClass(Service.class)
                    .addClass(RestService.class)
                    .addClass(Fruit.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    static MockTracer mockTracer = new MockTracer();
    static {
        GlobalTracer.registerIfAbsent(mockTracer);
    }

    @BeforeEach
    public void before() {
        mockTracer.reset();
    }

    @AfterAll
    public static void afterAll() {
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @Test
    public void testSingleServerRequest() {
        try {
            RestAssured.defaultParser = Parser.TEXT;
            RestAssured.when().get("/hello")
                    .then()
                    .statusCode(200);
            Assertions.assertEquals(1, mockTracer.finishedSpans().size());
            Assertions.assertEquals("GET:io.quarkus.smallrye.opentracing.deployment.TestResource.hello",
                    mockTracer.finishedSpans().get(0).operationName());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void testCDI() {
        try {
            RestAssured.defaultParser = Parser.TEXT;
            RestAssured.when().get("/cdi")
                    .then()
                    .statusCode(200);
            Assertions.assertEquals(2, mockTracer.finishedSpans().size());
            Assertions.assertEquals("io.quarkus.smallrye.opentracing.deployment.Service.foo",
                    mockTracer.finishedSpans().get(0).operationName());
            Assertions.assertEquals("GET:io.quarkus.smallrye.opentracing.deployment.TestResource.cdi",
                    mockTracer.finishedSpans().get(1).operationName());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void testMPRestClient() {
        try {
            RestAssured.defaultParser = Parser.TEXT;
            RestAssured.when().get("/restClient")
                    .then()
                    .statusCode(200);
            Assertions.assertEquals(3, mockTracer.finishedSpans().size());
            Assertions.assertEquals("GET:io.quarkus.smallrye.opentracing.deployment.TestResource.hello",
                    mockTracer.finishedSpans().get(0).operationName());
            Assertions.assertEquals("GET", mockTracer.finishedSpans().get(1).operationName());
            Assertions.assertEquals("GET:io.quarkus.smallrye.opentracing.deployment.TestResource.restClient",
                    mockTracer.finishedSpans().get(2).operationName());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void testContextPropagationInFaultTolerance() {
        try {
            RestAssured.defaultParser = Parser.TEXT;
            RestAssured.when().get("/faultTolerance")
                    .then()
                    .statusCode(200)
                    .body(equalTo("fallback"));
            Awaitility.await().atMost(5, TimeUnit.SECONDS)
                    .until(() -> mockTracer.finishedSpans().size() == 5);
            List<MockSpan> spans = mockTracer.finishedSpans();

            Assertions.assertEquals(5, spans.size());
            for (MockSpan mockSpan : spans) {
                Assertions.assertEquals(spans.get(0).context().traceId(), mockSpan.context().traceId());
            }

            // if timeout occurs, subsequent retries/fallback can be interleaved with the execution that timed out,
            // resulting in varying span order
            Assertions.assertEquals(3, countSpansWithOperationName(spans, "ft"));
            Assertions.assertEquals(1, countSpansWithOperationName(spans, "fallback"));
            Assertions.assertEquals(1, countSpansWithOperationName(spans,
                    "GET:io.quarkus.smallrye.opentracing.deployment.TestResource.faultTolerance"));
        } finally {
            RestAssured.reset();
        }
    }

    private long countSpansWithOperationName(List<MockSpan> spans, String operationName) {
        return spans.stream().filter(span -> span.operationName().equals(operationName)).count();
    }

    @Test
    public void testJPA() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/jpa")
                    .then()
                    .statusCode(200)
                    .body("", hasSize(3))
                    .body("name[0]", equalTo("Apple"))
                    .body("name[1]", equalTo("Banana"))
                    .body("name[2]", equalTo("Cherry"));
            List<MockSpan> spans = mockTracer.finishedSpans();

            Assertions.assertEquals(3, spans.size());
            for (MockSpan mockSpan : spans) {
                Assertions.assertEquals(spans.get(0).context().traceId(), mockSpan.context().traceId());
            }
            MockSpan firstSpan = mockTracer.finishedSpans().get(0);
            Assertions.assertEquals("Query", firstSpan.operationName());
            Assertions.assertTrue(firstSpan.tags().containsKey("db.statement"));
            Assertions.assertTrue(firstSpan.tags().get("db.statement").toString().contains("known_fruits"));
            Assertions.assertEquals("io.quarkus.smallrye.opentracing.deployment.Service.getFruits",
                    mockTracer.finishedSpans().get(1).operationName());
            Assertions.assertEquals("GET:io.quarkus.smallrye.opentracing.deployment.TestResource.jpa",
                    mockTracer.finishedSpans().get(2).operationName());
        } finally {
            RestAssured.reset();
        }
    }
}
