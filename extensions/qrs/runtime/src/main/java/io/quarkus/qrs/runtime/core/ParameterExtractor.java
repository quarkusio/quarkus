package io.quarkus.qrs.runtime.core;

public interface ParameterExtractor {

    /**
     * Extracts a parameter from the request.
     *
     * If this returns a {@link java.util.concurrent.CompletionStage} then the value will be the result of the
     * stage at the time it completes.
     *
     * @param context
     * @return
     */
    Object extractParameter(RequestContext context);

}
