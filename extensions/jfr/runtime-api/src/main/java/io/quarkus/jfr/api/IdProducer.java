package io.quarkus.jfr.api;

/**
 * An abstract access point for retrieving the {@code trace ID} and {@code span ID}
 * associated with the currently executing operation.
 * <p>
 * This interface provides a vendor- and implementation-neutral way to obtain
 * tracing identifiers that represent the execution context of the current thread
 * or request.
 */
public interface IdProducer {

    /**
     * Returns the unique identifier of the trace associated with the current execution.
     * <p>
     * A trace ID represents an end-to-end request or workflow and remains consistent
     * across service boundaries. By correlating events using this ID, it is possible
     * to understand where and how a request was processed throughout the entire system.
     *
     * @return the trace ID of the current execution, or {@code null} if no trace is active
     */
    String getTraceId();

    /**
     * Returns the identifier of the current span within the trace.
     * <p>
     * A span represents a single unit of work within a trace, such as a method invocation
     * or an HTTP request. Span IDs are typically used to analyze execution latency,
     * identify performance bottlenecks, and locate errors within a specific part
     * of the overall trace.
     *
     * @return the span ID of the current execution, or {@code null} if no span is active
     */
    String getSpanId();
}