package io.quarkus.micrometer.runtime.export.exemplars;

import java.util.function.Function;

public interface OpenTelemetryContextUnwrapper {
    /**
     * Called when an HTTP server response has ended.
     * Makes sure exemplars are produced because they have an OTel context.
     *
     * @param methodReference Ex: Sample stop method reference
     * @param parameter The parameter to pass to the method
     * @param requestContext The request context
     * @param <P> The parameter type is a type of metric, ex: Timer
     * @param <R> The return type of the method pointed by the methodReference
     * @return The result of the method
     */
    <P, R> R executeInContext(Function<P, R> methodReference, P parameter, io.vertx.core.Context requestContext);
}
