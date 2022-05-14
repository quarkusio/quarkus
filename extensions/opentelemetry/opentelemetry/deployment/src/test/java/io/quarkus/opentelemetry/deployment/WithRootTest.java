package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.runtime.tracing.cdi.WithRoot;
import io.quarkus.test.QuarkusUnitTest;

public class WithRootTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(SpanBean.class).addClass(TestSpanExporter.class));

    @Inject
    SpanBean spanBean;
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void span() {
        spanBean.nestedRoot();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);

        final SpanData withRoot = spanItems.get(0);
        final SpanData nestedRoot = spanItems.get(1);

        assertThat(withRoot.getName()).isEqualTo("SpanBean.withRoot");
        assertThat(nestedRoot.getName()).isEqualTo("SpanBean.nestedRoot");

        assertThat(withRoot.getParentSpanContext()).isEqualTo(Span.fromContext(Context.root()).getSpanContext());

    }

    @ApplicationScoped
    public static class SpanBean {

        @Inject
        SpanChildBean spanChildBean;

        @WithSpan
        public void nestedRoot() {
            spanChildBean.withRoot();
        }
    }

    @ApplicationScoped
    public static class SpanChildBean {
        @WithRoot
        @WithSpan
        public void withRoot() {

        }
    }
}
