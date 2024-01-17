package io.quarkus.opentelemetry.runtime.tracing.intrumentation.scheduler;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.ErrorCauseExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.quarkus.scheduler.spi.JobInstrumenter;

@Singleton
public class OpenTelemetryJobInstrumenter implements JobInstrumenter {

    private final Instrumenter<JobInstrumentationContext, Void> instrumenter;

    public OpenTelemetryJobInstrumenter(OpenTelemetry openTelemetry) {
        InstrumenterBuilder<JobInstrumentationContext, Void> instrumenterBuilder = Instrumenter.builder(
                openTelemetry, "io.quarkus.opentelemetry",
                new SpanNameExtractor<JobInstrumentationContext>() {
                    @Override
                    public String extract(JobInstrumentationContext context) {
                        return context.getSpanName();
                    }
                });
        instrumenterBuilder.setErrorCauseExtractor(new ErrorCauseExtractor() {
            @Override
            public Throwable extract(Throwable throwable) {
                return throwable;
            }
        });
        this.instrumenter = instrumenterBuilder.buildInstrumenter();
    }

    @Override
    public CompletionStage<Void> instrument(JobInstrumentationContext instrumentationContext) {
        Context parentCtx = Context.current();
        Context context = instrumenter.start(parentCtx, instrumentationContext);
        try (Scope scope = context.makeCurrent()) {
            return instrumentationContext
                    .executeJob()
                    .whenComplete(
                            (result, throwable) -> instrumenter.end(context, instrumentationContext, null, throwable));
        }
    }

}
