package io.quarkus.micrometer.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.prometheus.metrics.model.snapshots.Exemplar;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.tracer.common.SpanContext;

public class Substitutions {

    public static final class OpentelemetryExemplarSamplerProviderIsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader()
                        .loadClass("io.micrometer.prometheusmetrics.PrometheusMeterRegistry");
                return true;
            } catch (Throwable var3) {
                return false;
            }
        }
    }

    @TargetClass(className = "io.prometheus.metrics.core.exemplars.ExemplarSampler", onlyWith = OpentelemetryExemplarSamplerProviderIsPresent.class)
    public static final class Target_ExemplarSampler {

        @Alias
        private SpanContext spanContext;

        @Substitute
        private Labels doSampleExemplar() {
            // No SpanContextSupplier needed because Quarkus has its own.
            try {
                if (spanContext != null) {
                    if (spanContext.isCurrentSpanSampled()) {
                        String spanId = spanContext.getCurrentSpanId();
                        String traceId = spanContext.getCurrentTraceId();
                        if (spanId != null && traceId != null) {
                            spanContext.markCurrentSpanAsExemplar();
                            return Labels.of(Exemplar.TRACE_ID, traceId, Exemplar.SPAN_ID, spanId);
                        }
                    }
                }
            } catch (NoClassDefFoundError ignored) {
                // ignore
            }
            return Labels.EMPTY;
        }
    }
}
