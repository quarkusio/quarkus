package io.quarkus.smallrye.opentracing.deployment;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

public class TracingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestResource.class)
                    .addClass(TracerRegistrar.class)
                    .addClass(Service.class)
                    .addClass(RestService.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    static MockTracer mockTracer = new MockTracer();

    @AfterEach
    public void after() {
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
    @Disabled("https://github.com/quarkusio/quarkus/issues/2187")
    public void testContextPropagationInFaultTolerance() {
        try {
            RestAssured.defaultParser = Parser.TEXT;
            Response response = RestAssured.when().get("/faultTolerance");
            response.then().statusCode(200);
            Assertions.assertEquals("fallback", response.body().asString());
            Awaitility.await().atMost(5, TimeUnit.SECONDS)
                    .until(() -> mockTracer.finishedSpans().size() == 5);
            List<MockSpan> spans = mockTracer.finishedSpans();

            Assertions.assertEquals(5, spans.size());
            for (MockSpan mockSpan : spans) {
                Assertions.assertEquals(spans.get(0).context().traceId(), mockSpan.context().traceId());
            }
            Assertions.assertEquals("ft", mockTracer.finishedSpans().get(0).operationName());
            Assertions.assertEquals("ft", mockTracer.finishedSpans().get(1).operationName());
            Assertions.assertEquals("ft", mockTracer.finishedSpans().get(2).operationName());
            Assertions.assertEquals("io.quarkus.smallrye.opentracing.deployment.Service.fallback",
                    mockTracer.finishedSpans().get(3).operationName());
            Assertions.assertEquals("io.quarkus.smallrye.opentracing.deployment.Service.faultTolerance",
                    mockTracer.finishedSpans().get(4).operationName());
        } finally {
            RestAssured.reset();
        }
    }
}
