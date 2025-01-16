package io.quarkus.opentelemetry.deployment.traces;

import static io.quarkus.opentelemetry.runtime.tracing.mutiny.MutinyTracingHelper.wrapWithSpan;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

class MutinyTracingHelperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.bsp.schedule.delay=50ms\n"),
                                    "application.properties"));

    @Inject
    private TestSpanExporter spanExporter;

    @Inject
    private Tracer tracer;

    @Inject
    private Vertx vertx;

    @BeforeEach
    void tearDown() {
        spanExporter.reset();
    }

    @ParameterizedTest(name = "{index}: Simple uni pipeline {1}")
    @MethodSource("generateContextRunners")
    void testSimpleUniPipeline(final String contextType, final String contextName) {

        final UniAssertSubscriber<String> subscriber = Uni.createFrom()
                .item("Hello")
                .emitOn(r -> runOnContext(r, vertx, contextType))
                .onItem()
                .transformToUni(m -> wrapWithSpan(tracer, "testSpan",
                        Uni.createFrom().item(m).onItem().transform(s -> {
                            final Span span = tracer.spanBuilder("subspan").startSpan();
                            try (final Scope scope = span.makeCurrent()) {
                                return s + " world";
                            } finally {
                                span.end();
                            }
                        })))
                .subscribe()
                .withSubscriber(new UniAssertSubscriber<>());

        subscriber.awaitItem().assertItem("Hello world");

        //ensure there are two spans with subspan as child of testSpan
        final List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertThat(spans.stream().map(SpanData::getName)).containsExactlyInAnyOrder("testSpan", "subspan");
        assertChildSpan(spans, "testSpan", "subspan");
    }

    @ParameterizedTest(name = "{index}: Explicit parent {1}")
    @MethodSource("generateContextRunners")
    void testSpanWithExplicitParent(final String contextType, final String contextName) {

        final String parentSpanName = "parentSpan";
        final String pipelineSpanName = "pipelineSpan";
        final String subspanName = "subspan";

        final Span parentSpan = tracer.spanBuilder(parentSpanName).startSpan();
        final io.opentelemetry.context.Context parentContext = io.opentelemetry.context.Context.current().with(parentSpan);

        final UniAssertSubscriber<String> subscriber = Uni.createFrom()
                .item("Hello")
                .emitOn(r -> runOnContext(r, vertx, contextType))
                .onItem()
                .transformToUni(m -> wrapWithSpan(tracer, Optional.of(parentContext),
                        pipelineSpanName,
                        Uni.createFrom().item(m).onItem().transform(s -> {
                            final Span span = tracer.spanBuilder(subspanName).startSpan();
                            try (final Scope scope = span.makeCurrent()) {
                                return s + " world";
                            } finally {
                                span.end();
                            }
                        })))
                .subscribe()
                .withSubscriber(new UniAssertSubscriber<>());

        subscriber.awaitItem().assertItem("Hello world");
        parentSpan.end();

        //ensure there are 3 spans with proper parent-child relationships
        final List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        assertThat(spans.stream().map(SpanData::getName)).containsExactlyInAnyOrder(parentSpanName, pipelineSpanName,
                subspanName);
        assertChildSpan(spans, parentSpanName, pipelineSpanName);
        assertChildSpan(spans, pipelineSpanName, subspanName);
    }

    @ParameterizedTest(name = "{index}: Nested uni pipeline with implicit parent {1}")
    @MethodSource("generateContextRunners")
    void testNestedPipeline_implicitParent(final String contextType,
            final String contextName) {

        final String parentSpanName = "parentSpan";
        final String childSpanName = "childSpan";

        final UniAssertSubscriber<String> subscriber = Uni.createFrom()
                .item("test")
                .emitOn(r -> runOnContext(r, vertx, contextType))
                .onItem()
                .transformToUni(m -> wrapWithSpan(tracer, parentSpanName,
                        Uni.createFrom().item(m)
                                .onItem().transform(s -> s + " in outer span")
                                .onItem().transformToUni(m1 -> wrapWithSpan(tracer, childSpanName,
                                        Uni.createFrom().item(m1)
                                                .onItem().transform(s -> "now in inner span")))

                ))
                .subscribe()
                .withSubscriber(new UniAssertSubscriber<>());

        subscriber.awaitItem(Duration.ofMillis(300));

        //ensure there are 2 spans with doSomething and doSomethingAsync as children of testSpan
        final List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertThat(spans.stream().map(SpanData::getName)).containsExactlyInAnyOrder(parentSpanName, childSpanName);
        assertChildSpan(spans, parentSpanName, childSpanName);
    }

    @ParameterizedTest(name = "{index}: Nested uni pipeline with explicit no parent {1}")
    @MethodSource("generateContextRunners")
    void testNestedPipeline_explicitNoParent(final String contextType, final String contextName) {

        final String parentSpanName = "parentSpan";
        final String childSpanName = "childSpan";

        final UniAssertSubscriber<String> subscriber = Uni.createFrom()
                .item("test")
                .emitOn(r -> runOnContext(r, vertx, contextType))
                .onItem()
                .transformToUni(m -> wrapWithSpan(tracer, parentSpanName,
                        Uni.createFrom().item(m)
                                .onItem().transform(s -> s + " in outer span")
                                .onItem().transformToUni(m1 -> wrapWithSpan(tracer, Optional.empty(), childSpanName,
                                        Uni.createFrom().item(m1)
                                                .onItem().transform(s -> "now in inner span")))

                ))
                .subscribe()
                .withSubscriber(new UniAssertSubscriber<>());

        subscriber.awaitItem(Duration.ofMillis(300));

        //ensure there are 2 spans but without parent-child relationship
        final List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertThat(spans.stream().map(SpanData::getName)).containsExactlyInAnyOrder(parentSpanName, childSpanName);
        assertThat(spans.stream()
                .filter(span -> span.getName().equals(childSpanName))
                .findAny()
                .orElseThrow()
                .getParentSpanId()).isEqualTo("0000000000000000");//signifies no parent
    }

    @ParameterizedTest(name = "{index}: Concatenating multi pipeline {1}")
    @MethodSource("generateContextRunners")
    void testSimpleMultiPipeline_Concatenate(final String contextType, final String contextName) {

        final AssertSubscriber<String> subscriber = Multi.createFrom()
                .items("test1", "test2", "test3")
                .emitOn(r -> runOnContext(r, vertx, contextType))
                .onItem()
                .transformToUniAndConcatenate(m -> wrapWithSpan(tracer, Optional.empty(), "testSpan " + m,
                        //the traced pipeline
                        Uni.createFrom().item(m).onItem().transform(s -> {
                            final Span span = tracer.spanBuilder("subspan " + s).startSpan();
                            try (final Scope scope = span.makeCurrent()) {
                                return s + " transformed";
                            } finally {
                                span.end();
                            }
                        })))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitCompletion().assertItems("test1 transformed", "test2 transformed", "test3 transformed");

        //ensure there are six spans with three pairs of subspan as child of testSpan
        final List<SpanData> spans = spanExporter.getFinishedSpanItems(6);
        for (int i = 1; i <= 3; i++) {
            final int currentI = i;
            assertThat(spans.stream().anyMatch(span -> span.getName().equals("testSpan test" + currentI))).isTrue();
            assertThat(spans.stream().anyMatch(span -> span.getName().equals("subspan test" + currentI))).isTrue();
            assertChildSpan(spans, "testSpan test" + currentI, "subspan test" + currentI);
        }
    }

    @ParameterizedTest(name = "{index}: Merging multi pipeline {1}")
    @MethodSource("generateContextRunners")
    void testSimpleMultiPipeline_Merge(final String contextType, final String contextName) {

        final AssertSubscriber<String> subscriber = Multi.createFrom()
                .items("test1", "test2", "test3")
                .emitOn(r -> runOnContext(r, vertx, contextType))
                .onItem()
                .transformToUniAndMerge(m -> wrapWithSpan(tracer, Optional.empty(), "testSpan " + m,
                        Uni.createFrom().item(m).onItem().transform(s -> {
                            final Span span = tracer.spanBuilder("subspan " + s).startSpan();
                            try (final Scope scope = span.makeCurrent()) {
                                return s + " transformed";
                            } finally {
                                span.end();
                            }
                        })))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitCompletion();

        //ensure there are six spans with three pairs of subspan as child of testSpan
        final List<SpanData> spans = spanExporter.getFinishedSpanItems(6);
        for (int i = 1; i <= 3; i++) {
            final int currentI = i;
            assertThat(spans.stream().anyMatch(span -> span.getName().equals("testSpan test" + currentI))).isTrue();
            assertThat(spans.stream().anyMatch(span -> span.getName().equals("subspan test" + currentI))).isTrue();
            assertChildSpan(spans, "testSpan test" + currentI, "subspan test" + currentI);
        }
    }

    private static void assertChildSpan(final List<SpanData> spans, final String parentSpanName,
            final String childSpanName1) {
        assertThat(spans.stream()
                .filter(span -> span.getName().equals(childSpanName1))
                .findAny()
                .orElseThrow()
                .getParentSpanId()).isEqualTo(
                        spans.stream().filter(span -> span.getName().equals(parentSpanName)).findAny().get().getSpanId());
    }

    private static Stream<Arguments> generateContextRunners() {
        return Stream.of(
                Arguments.of("WITHOUT_CONTEXT", "Without Context"),
                Arguments.of("ROOT_CONTEXT", "On Root Context"),
                Arguments.of("DUPLICATED_CONTEXT", "On Duplicated Context"));
    }

    private void runOnContext(final Runnable runnable, final Vertx vertx, final String contextType) {
        switch (contextType) {
            case "WITHOUT_CONTEXT":
                runWithoutContext(runnable);
                break;
            case "ROOT_CONTEXT":
                runOnRootContext(runnable, vertx);
                break;
            case "DUPLICATED_CONTEXT":
                runOnDuplicatedContext(runnable, vertx);
                break;
            default:
                throw new IllegalArgumentException("Unknown context type: " + contextType);
        }
    }

    private static void runWithoutContext(final Runnable runnable) {
        assertThat(QuarkusContextStorage.getVertxContext()).isNull();
        runnable.run();
    }

    private static void runOnRootContext(final Runnable runnable, final Vertx vertx) {
        final Context rootContext = VertxContext.getRootContext(vertx.getOrCreateContext());
        assertThat(rootContext).isNotNull();
        assertThat(VertxContext.isDuplicatedContext(rootContext)).isFalse();
        assertThat(rootContext).isNotEqualTo(QuarkusContextStorage.getVertxContext());

        rootContext.runOnContext(v -> runnable.run());
    }

    private static void runOnDuplicatedContext(final Runnable runnable, final Vertx vertx) {
        final Context duplicatedContext = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertThat(duplicatedContext).isNotNull();
        assertThat(VertxContext.isDuplicatedContext(duplicatedContext)).isTrue();

        duplicatedContext.runOnContext(v -> runnable.run());
    }

}
