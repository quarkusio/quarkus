package io.quarkus.opentelemetry.deployment.traces;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.Executor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryScopeTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(TestUtil.class)
                            .addClass(HelloResource.class)
                            .addClass(HelloBean.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties");

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void telemetry() {
        final int requests = 1;

        RestAssured.given()
                .pathParam("reqs", requests)
                .when()
                .get("/hello/many-scopes/{reqs}").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(requests * 2 + 1);

        assertEquals(1, spans.stream()
                .filter(spanData -> spanData.getName().equals("GET /hello/many-scopes/{reqs}"))
                .collect(toList())
                .size());

        List<SpanData> incomingSpans = spans.stream()
                .filter(spanData -> spanData.getName().equals("HelloBean.manyScopes"))
                .collect(toList());
        assertEquals(requests, incomingSpans.size());
        //        incomingSpans.stream()
        //                .map(spanData -> spanData.getAttributes())
        //                .map(attributes -> attributes.asMap())

        List<SpanData> childSpans = spans.stream()
                .filter(spanData -> spanData.getName().equals("child-span-pair-scopes"))
                .collect(toList());
        assertEquals(requests, childSpans
                .size());
    }

    @Path("/hello")
    public static class HelloResource {
        @Inject
        HelloBean helloBean;

        @Inject
        Executor executor;

        @GET
        public String hello() {
            return helloBean.helloWithSpan();
        }

        @GET
        @Path("/withSpan")
        @WithSpan("withSpan")
        public String withSpan() {
            return helloBean.helloWithSpan();
        }

        @GET
        @Path("/many-scopes/{reqs}")
        public String manyScopes(@PathParam("reqs") int requests) {

            for (int i = 0; i < requests; i++) {
                executor.execute(() -> helloBean.manyScopes());
            }
            return "hello";
        }
    }

    @ApplicationScoped
    public static class HelloBean {

        @Inject
        Tracer tracer;

        @WithSpan
        public String helloWithSpan() {
            return "hello";
        }

        @WithSpan
        public String manyScopes() {
            final Span inbound = Span.current();
            final String inboundName = "HelloBean.manyScopes";
            final String childName = "child-span-pair-scopes";
            final Span child = tracer.spanBuilder(childName).startSpan();

            recordScopeInCurrentSpan(inboundName, 1);

            try (Scope scope2 = child.makeCurrent()) {
                recordScopeInCurrentSpan(childName, 2);
                try (Scope scope3 = inbound.makeCurrent()) {
                    recordScopeInCurrentSpan(inboundName, 3);
                }
                recordScopeInCurrentSpan(childName, 2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try (Scope scope4 = child.makeCurrent()) {
                recordScopeInCurrentSpan(childName, 4);
                try (Scope scope5 = inbound.makeCurrent()) {
                    recordScopeInCurrentSpan(inboundName, 5);
                }
                recordScopeInCurrentSpan(childName, 4);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                child.end();
            }

            try (Scope scope6 = child.makeCurrent()) {
                recordScopeInCurrentSpan(childName, 6);// invalid. Span closed.
                try (Scope scope7 = inbound.makeCurrent()) {
                    recordScopeInCurrentSpan(inboundName, 7);
                }
                recordScopeInCurrentSpan(childName, 6); // invalid. Span closed.
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            recordScopeInCurrentSpan(inboundName, 1);

            return "hello";
        }

        private static Span recordScopeInCurrentSpan(String expectedSpanName, long scopeId) {
            return Span.current().addEvent("current is " + expectedSpanName + " - scope " + scopeId,
                    Attributes.of(
                            AttributeKey.stringKey("expectedSpanName"), expectedSpanName,
                            AttributeKey.longKey("scope"), scopeId));
        }
    }
}
