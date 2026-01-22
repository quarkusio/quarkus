package io.quarkus.opentelemetry.deployment.traces;

import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
        RestAssured.when()
                .get("/hello/many-scopes").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

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
        @Path("/many-scopes")
        public String manyScopes() {

            int requests = 1;
            for (int i = 0; i < requests; i++){
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
            Span inbound = Span.current();
            Span child = tracer.spanBuilder("child-span").startSpan();

            Span.current().addEvent("current is inbound - scope 1");

            try (Scope scope2 = child.makeCurrent()) {
                Span.current().addEvent("current is child - scope 2");

                try (Scope scope3 = inbound.makeCurrent()) {
                    Span.current().addEvent("current is inbound - scope 3");
                }

                Span.current().addEvent("current is child - scope 2");

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try (Scope scope4 = child.makeCurrent()) {
                Span.current().addEvent("current is child - scope 4");

                try (Scope scope5 = inbound.makeCurrent()) {
                    Span.current().addEvent("current is inbound - scope 5");
                }

                Span.current().addEvent("current is child - scope 4");

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                child.end();
            }

            try (Scope scope6 = child.makeCurrent()) {
                Span.current().addEvent("current is child - scope 6"); // invalid. Span closed.

                try (Scope scope7 = inbound.makeCurrent()) {
                    Span.current().addEvent("current is inbound - scope 7");
                }

                Span.current().addEvent("current is child - scope 6"); // invalid. Span closed.

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Span.current().addEvent("current is inbound - scope 1");

            return "hello";
        }
    }
}
