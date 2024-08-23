package io.quarkus.scheduler.spi;

import java.util.concurrent.CompletionStage;

/**
 * Instruments a scheduled job.
 * <p>
 * Telemetry extensions can provide exactly one CDI bean of this type. The scope must be either {@link jakarta.inject.Singleton}
 * or {@link jakarta.enterprise.context.ApplicationScoped}.
 */
public interface JobInstrumenter {

    CompletionStage<Void> instrument(JobInstrumentationContext context);

    interface JobInstrumentationContext {

        String getSpanName();

        CompletionStage<Void> executeJob();

    }

}
