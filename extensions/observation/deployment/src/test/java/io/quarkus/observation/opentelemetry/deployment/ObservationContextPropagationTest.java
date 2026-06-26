package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ObservationContextPropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class,
                                    TestResource.class, ChildService.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.traces.exporter=test-span-exporter\n" +
                                            "quarkus.otel.bsp.schedule.delay=50ms\n" +
                                            "quarkus.otel.metrics.exporter=none\n"),
                                    "application.properties"));

    @Inject
    TestObservationRegistry registry;

    @Inject
    ThreadContext threadContext;

    @Inject
    TestSpanExporter spanExporter;

    static final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void tearDown() {
        registry.clear();
        spanExporter.reset();
    }

    private void awaitObservations(int count) {
        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> TestObservationRegistryAssert.assertThat(registry)
                        .hasNumberOfObservationsEqualTo(count));
    }

    @Test
    void observationPropagatedToAnotherThread() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Observation observation = Observation.createNotStarted("propagation-test", registry);
            observation.start();
            Observation.Scope scope = observation.openScope();

            assertThat(registry.getCurrentObservation()).isNotNull();

            CompletableFuture<Observation> result = threadContext
                    .withContextCapture(CompletableFuture.completedFuture("trigger"))
                    .thenApplyAsync(s -> registry.getCurrentObservation(), executor);

            Observation propagated = result.get(5, TimeUnit.SECONDS);

            scope.close();
            observation.stop();

            assertThat(propagated).isSameAs(observation);

            List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
            assertThat(spans.get(0).getName()).isEqualTo("propagation-test");

            Timer timer = simpleMeterRegistry.find("propagation-test").timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void observationNotPropagatedWithoutContext() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Observation observation = Observation.createNotStarted("no-propagation-test", registry);
            observation.start();
            Observation.Scope scope = observation.openScope();

            CompletableFuture<Observation> result = CompletableFuture.supplyAsync(
                    () -> registry.getCurrentObservation(), executor);

            Observation propagated = result.get(5, TimeUnit.SECONDS);

            scope.close();
            observation.stop();

            assertThat(propagated).isNull();

            List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
            assertThat(spans.get(0).getName()).isEqualTo("no-propagation-test");

            Timer timer = simpleMeterRegistry.find("no-propagation-test").timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void parentChildSpansAcrossThreadsViaRestEndpoint() {
        String body = RestAssured.when()
                .get("/observation/propagation")
                .then()
                .statusCode(200)
                .extract().asString();

        assertThat(body).isEqualTo("child-result");

        awaitObservations(2);
        TestObservationRegistryAssert.assertThat(registry)
                .hasNumberOfObservationsEqualTo(2)
                .hasObservationWithNameEqualTo("doWork")
                .that()
                .hasLowCardinalityKeyValue("code.function", "doWork")
                .hasLowCardinalityKeyValue("code.namespace", TestResource.class.getName())
                .doesNotHaveParentObservation()
                .hasBeenStarted()
                .hasBeenStopped()
                .backToTestObservationRegistry()
                .hasObservationWithNameEqualTo("doChildWork")
                .that()
                .hasLowCardinalityKeyValue("code.function", "doChildWork")
                .hasLowCardinalityKeyValue("code.namespace", ChildService.class.getName())
                .hasParentObservationContextMatching(ctx -> "doWork".equals(ctx.getName()))
                .hasBeenStarted()
                .hasBeenStopped();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        SpanData parentSpan = spans.stream()
                .filter(s -> "TestResource#doWork".equals(s.getName()))
                .findFirst().orElseThrow();
        SpanData childSpan = spans.stream()
                .filter(s -> "ChildService#doChildWork".equals(s.getName()))
                .findFirst().orElseThrow();

        assertThat(childSpan.getTraceId()).isEqualTo(parentSpan.getTraceId());
        assertThat(childSpan.getParentSpanId()).isEqualTo(parentSpan.getSpanId());

        Timer doWorkTimer = simpleMeterRegistry.find("doWork")
                .tag("code.function", "doWork")
                .timer();
        assertThat(doWorkTimer).isNotNull();
        assertThat(doWorkTimer.count()).isEqualTo(1);

        Timer doChildWorkTimer = simpleMeterRegistry.find("doChildWork")
                .tag("code.function", "doChildWork")
                .timer();
        assertThat(doChildWorkTimer).isNotNull();
        assertThat(doChildWorkTimer.count()).isEqualTo(1);
    }

    @ApplicationScoped
    @Path("/observation")
    public static class TestResource {

        @Inject
        ChildService childService;

        @Inject
        ThreadContext threadContext;

        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        @GET
        @Path("/propagation")
        @Observed
        public CompletionStage<String> doWork() {
            return threadContext
                    .withContextCapture(CompletableFuture.completedFuture("trigger"))
                    .thenApplyAsync(s -> childService.doChildWork(), executor);
        }
    }

    @ApplicationScoped
    public static class ChildService {

        @Observed
        public String doChildWork() {
            return "child-result";
        }
    }

    @ApplicationScoped
    public static class TestRegistryProducer {

        @Produces
        @Singleton
        @Alternative
        @Unremovable
        @Priority(Integer.MAX_VALUE)
        TestObservationRegistry testObservationRegistry(OpenTelemetryObservationHandler tracingHandler) {
            Metrics.addRegistry(simpleMeterRegistry);
            TestObservationRegistry registry = TestObservationRegistry.create();
            registry.observationConfig().observationHandler(tracingHandler);
            registry.observationConfig().observationHandler(
                    new DefaultMeterObservationHandler(simpleMeterRegistry));
            return registry;
        }
    }
}
