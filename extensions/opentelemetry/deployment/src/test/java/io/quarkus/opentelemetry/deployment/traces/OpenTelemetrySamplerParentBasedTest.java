package io.quarkus.opentelemetry.deployment.traces;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetrySamplerParentBasedTest {

    private static final Logger logger = Logger.getLogger(OpenTelemetrySamplerParentBasedTest.class);

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class, HelloResource.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties")
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.opentelemetry\".level", "DEBUG")
            .overrideConfigKey("quarkus.otel.traces.sampler", "parentbased_always_on")
            .overrideConfigKey("quarkus.otel.traces.suppress-application-uris", "/hello");

    @Inject
    TestSpanExporter spanExporter;

    @BeforeEach
    void init() {
        spanExporter.reset();
    }

    /**
     * FIXME This test currently fails on purpose
     *
     * When span count is 0, Sample happened but error was not reproduced.
     * When span count is 1, Sampling happened and it reproduces the error. Not following the parent span sampling directive.
     * When span count is 3, No sampling occurred.
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws InterruptedException
     */
    @RepeatedTest(5)
    void test() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        await().pollDelay(500, MILLISECONDS)
                .atMost(2, SECONDS)
                .untilAsserted(() -> {
                    List<SpanData> spans = spanExporter.getPartialFinishedSpanItems();
                    assertEquals(0, spans.size());
                });
    }

    @Path("/hello")
    public static class HelloResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            test1();
            test2();
            return "hello";
        }

        @WithSpan
        void test1() {
            //nothing
        }

        @WithSpan
        void test2() {
            //nothing
        }
    }
}
