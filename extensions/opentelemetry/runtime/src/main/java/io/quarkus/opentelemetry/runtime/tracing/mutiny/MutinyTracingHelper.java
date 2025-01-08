package io.quarkus.opentelemetry.runtime.tracing.mutiny;

import java.util.Optional;
import java.util.concurrent.CancellationException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class MutinyTracingHelper {

    /**
     * Wraps the given pipeline with a span with the given name. Ensures that subspans find the current span as context,
     * by running on a duplicated context. The span will be closed when the pipeline completes.
     * If there is already a span in the current context, it will be used as parent for the new span.
     * <p>
     * Use as follows:
     * Given this existing pipeline:
     * ```java
     * Uni.createFrom().item("Hello")
     * .onItem().transform(s -> s + " World")
     * .subscribe().with(System.out::println);
     * ```
     * wrap like this:
     * ```java
     * Uni.createFrom().item("Hello")
     * .onItem().transformToUni(s -> wrapWithSpan(tracer, "mySpan", Uni.createFrom().item(s + " World")))
     * .subscribe().with(System.out::println);
     * ```
     * <p>
     * it also works with multi:
     * ```java
     * Multi.createFrom().items("Alice", "Bob", "Charlie")
     * .onItem().transform(name -> "Hello " + name)
     * .subscribe().with(System.out::println);
     * ```
     * wrap like this:
     * ```java
     * Multi.createFrom().items("Alice", "Bob", "Charlie")
     * .onItem().transformToUni(s -> wrapWithSpan(tracer, "mySpan", Uni.createFrom().item("Hello " + s)
     * .onItem().transform(name -> "Hello " + name)
     * ))
     * .subscribe().with(System.out::println);
     * ```
     *
     * @param <T> the type of the result of the pipeline
     * @param spanName
     *        the name of the span that should be created
     * @param pipeline
     *        the pipeline to run within the span
     *
     * @return the result of the pipeline
     */
    public static <T> Uni<T> wrapWithSpan(final Tracer tracer, final String spanName, final Uni<T> pipeline) {

        return wrapWithSpan(tracer, Optional.of(io.opentelemetry.context.Context.current()), spanName, pipeline);
    }

    /**
     * see {@link #wrapWithSpan(Tracer, String, Uni)}
     * use this method if you manually want to specify the parent context of the new span
     * or if you want to ensure the new span is a root span.
     *
     * @param <T>
     * @param parentContext
     *        the parent context to use for the new span. If empty, a new root span will be created.
     * @param spanName
     *        the name of the span that should be created
     * @param pipeline
     *        the pipeline to run within the span
     *
     * @return the result of the pipeline
     */
    public static <T> Uni<T> wrapWithSpan(final Tracer tracer,
            final Optional<io.opentelemetry.context.Context> parentContext,
            final String spanName, final Uni<T> pipeline) {

        return runOnDuplicatedContext(Uni.createFrom().deferred(() -> {
            final SpanBuilder spanBuilder = tracer.spanBuilder(spanName);
            if (parentContext.isPresent()) {
                spanBuilder.setParent(parentContext.get());
            } else {
                spanBuilder.setNoParent();
            }
            final Span span = spanBuilder.startSpan();
            final Scope scope = span.makeCurrent();
            return pipeline.onTermination()
                    .invoke((o, throwable, isCancelled) -> {
                        try {
                            if (Boolean.TRUE.equals(isCancelled)) {
                                span.recordException(new CancellationException());
                            } else if (throwable != null) {
                                span.recordException(throwable);
                            }
                            span.end();
                        } finally {
                            scope.close();
                        }
                    });
        }));
    }

    private static <T> Uni<T> runOnDuplicatedContext(final Uni<T> deferred) {
        //creates duplicate context, if the current context  is not a duplicated one and not null
        //Otherwise returns the current context or null
        final Context context = QuarkusContextStorage.getVertxContext();

        return deferred.runSubscriptionOn(runnable -> {
            if (context != null) {
                context.runOnContext(v -> runnable.run());
            } else {
                runnable.run();
            }
        });
    }

}
