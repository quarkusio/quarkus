package io.quarkus.smallrye.opentracing.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

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
        System.out.println(GlobalTracer.get());
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
}
