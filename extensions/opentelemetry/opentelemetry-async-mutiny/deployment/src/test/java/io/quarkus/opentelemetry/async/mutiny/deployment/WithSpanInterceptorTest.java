package io.quarkus.opentelemetry.async.mutiny.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategies;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.OpenTelemetryMultiInterceptor;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.OpenTelemetryUniInterceptor;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.TracingMulti;
import io.quarkus.opentelemetry.async.mutiny.runtime.tracing.TracingUni;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class WithSpanInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.async.mutiny.event.on-subscribe", "MyCustomText")
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(SpanBean.class)
                            .addClass(TestSpanExporter.class)
                            .addAsResource(
                                    new StringAsset(OpenTelemetryUniInterceptor.class.getCanonicalName()),
                                    "META-INF/services/io.smallrye.mutiny.infrastructure.UniInterceptor")
                            .addAsResource(
                                    new StringAsset(OpenTelemetryMultiInterceptor.class.getCanonicalName()),
                                    "META-INF/services/io.smallrye.mutiny.infrastructure.MultiInterceptor"));

    @Inject
    SpanBean spanBean;
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void testAsyncStrategies() {
        assertThat(AsyncOperationEndStrategies.instance().resolveStrategy(Uni.class)).isNotNull();
        assertThat(AsyncOperationEndStrategies.instance().resolveStrategy(Multi.class)).isNotNull();

        // TODO: Not sure how much sense this makes to test this if it is registered above.
        assertThat(Uni.createFrom().item("test")).isInstanceOf(TracingUni.class);
        assertThat(Multi.createFrom().item("test")).isInstanceOf(TracingMulti.class);
    }

    @Test
    void span() {
        spanBean.span().subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.span", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void spanName() {
        spanBean.spanName().subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("name", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void spanKind() {
        spanBean.spanKind().subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.spanKind", spanItems.get(0).getName());
        assertEquals(SERVER, spanItems.get(0).getKind());
    }

    @Test
    void spanArgs() {
        spanBean.spanArgs("argument").subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.spanArgs", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
        assertEquals("argument", spanItems.get(0).getAttributes().get(AttributeKey.stringKey("arg")));
    }

    @Test
    void spanChild() {
        spanBean.spanChild().subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(2);
        assertEquals("SpanChildBean.spanChild", spanItems.get(0).getName());
        assertEquals("SpanBean.spanChild", spanItems.get(1).getName());
        assertEquals(spanItems.get(0).getParentSpanId(), spanItems.get(1).getSpanId());
    }

    @Test
    void multiSpan() {
        spanBean.multiSpan().subscribe().withSubscriber(AssertSubscriber.create(3))
                .awaitCompletion().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.multiSpan", spanItems.get(0).getName());
    }

    @Test
    void multiSpanConcat() {
        spanBean.multiSpanConcat().subscribe().withSubscriber(AssertSubscriber.create(6))
                .awaitCompletion().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(3);

        final SpanData stream2 = spanItems.get(0);
        final SpanData stream1 = spanItems.get(1);
        final SpanData parent = spanItems.get(2);

        assertEquals("SpanChildBean.stream2", stream2.getName());
        assertEquals("SpanChildBean.stream1", stream1.getName());
        assertEquals("SpanBean.multiSpanConcat", parent.getName());

        assertThat(stream2.getEndEpochNanos()).isGreaterThan(stream1.getEndEpochNanos());
        assertThat(stream2.getParentSpanId()).isEqualTo(parent.getSpanId());
        assertThat(stream1.getParentSpanId()).isEqualTo(parent.getSpanId());
    }

    @Test
    void multiSpanMerge() {
        spanBean.multiSpanMerge().subscribe().withSubscriber(AssertSubscriber.create(6))
                .awaitCompletion().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(3);

        final SpanData stream2 = spanItems.get(0);
        final SpanData stream1 = spanItems.get(1);
        final SpanData parent = spanItems.get(2);

        assertEquals("SpanChildBean.stream2", stream2.getName());
        assertEquals("SpanChildBean.stream1", stream1.getName());
        assertEquals("SpanBean.multiSpanMerge", parent.getName());

        // TODO: Not sure if we can verify that. In theory we do not know what end earlier.
        //  we only know the order the spans are registered because we register on stream creation. This is actually
        //  what OpenTelemetry also does for other reactive libraries.
        assertThat(stream2.getEndEpochNanos()).isLessThan(stream1.getEndEpochNanos());
        assertThat(stream2.getParentSpanId()).isEqualTo(parent.getSpanId());
        assertThat(stream1.getParentSpanId()).isEqualTo(parent.getSpanId());
    }

    @Test
    void subscriptionEvent() {
        spanBean.span().subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().assertCompleted();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);

        final SpanData span = spanItems.get(0);
        assertEquals("SpanBean.span", span.getName());

        assertThat(span.getEvents()).hasSize(1);
        assertThat(span.getEvents().get(0).getName()).isEqualTo("MyCustomText");
    }

    @Test
    void cancellationAttribute() {
        spanBean.slow().subscribe().withSubscriber(UniAssertSubscriber.create()).cancel();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);

        final SpanData span = spanItems.get(0);
        assertEquals("SpanBean.slow", span.getName());

        assertThat(span.getAttributes().size()).isEqualTo(1);
        assertThat(span.getAttributes().get(AttributeKey.booleanKey("mutiny.canceled"))).isNotNull().isTrue();
    }

    @ApplicationScoped
    public static class SpanBean {
        @WithSpan
        public Uni<String> span() {
            return createDefaultUni();
        }

        @WithSpan
        public Uni<String> slow() {
            return createDefaultUni(10000);
        }

        @WithSpan("name")
        public Uni<String> spanName() {
            return createDefaultUni();
        }

        @WithSpan(kind = SERVER)
        public Uni<String> spanKind() {
            return createDefaultUni();
        }

        @WithSpan
        public Uni<String> spanArgs(@SpanAttribute(value = "arg") String arg) {
            return createDefaultUni();
        }

        @Inject
        SpanChildBean spanChildBean;

        @WithSpan
        public Uni<String> spanChild() {
            return spanChildBean.spanChild();
        }

        @WithSpan
        public Multi<String> multiSpan() {
            return createDefaultMulti();
        }

        @WithSpan
        public Multi<String> multiSpanConcat() {
            return Multi.createBy().concatenating().streams(spanChildBean.stream1(), spanChildBean.stream2());
        }

        @WithSpan
        public Multi<String> multiSpanMerge() {
            return Multi.createBy().merging().streams(spanChildBean.stream1(), spanChildBean.stream2());
        }
    }

    @ApplicationScoped
    public static class SpanChildBean {

        @WithSpan
        public Uni<String> spanChild() {
            return createDefaultUni();
        }

        @WithSpan
        public Multi<String> stream1() {
            return createDefaultMulti(200);
        }

        @WithSpan
        public Multi<String> stream2() {
            return createDefaultMulti(100);
        }
    }

    private static Uni<String> createDefaultUni() {
        return createDefaultUni(100);
    }

    private static Uni<String> createDefaultUni(final int delayMs) {
        return createDefaultUni(getCurrentSpanContext(), delayMs);
    }

    private static Uni<String> createDefaultUni(final SpanContext context) {
        return createDefaultUni(context, 100);
    }

    private static Uni<String> createDefaultUni(final SpanContext context, final int delayMs) {
        return Uni.createFrom().item("QUARKUS").onItem().delayIt().by(Duration.ofMillis(delayMs)).onItem()
                .invoke(() -> assertThat(Span.current().getSpanContext()).isEqualTo(context));
    }

    private static Multi<String> createDefaultMulti() {
        return createDefaultMulti(100);
    }

    private static Multi<String> createDefaultMulti(final SpanContext context) {
        return createDefaultMulti(context, 100);
    }

    private static Multi<String> createDefaultMulti(final int delayMs) {
        return createDefaultMulti(getCurrentSpanContext(), delayMs);
    }

    private static Multi<String> createDefaultMulti(final SpanContext context, final int delayMs) {
        return Multi.createFrom().items("SUPERSONIC", "SUBATOMIC", "JAVA")
                .onItem().call(() -> Uni.createFrom()
                        .voidItem().onItem().delayIt().by(Duration.ofMillis(delayMs)))
                // Verify that we have the expected context.
                .onItem().invoke(() -> assertThat(Span.current().getSpanContext()).isEqualTo(context));
    }

    private static SpanContext getCurrentSpanContext() {
        return Span.fromContext(Context.current()).getSpanContext();
    }
}
