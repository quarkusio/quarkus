package io.quarkus.qute;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.operators.AbstractUni;

class CompletionStageSupport {

    static final String SYSTEM_PROPERTY = "quarkus.qute.unrestricted-completion-stage-support";

    /**
     * {@code true} if any {@link CompletionStage} implementation is supported. {@code false} if only {@link CompletableFuture}
     * and {@link CompletedStage} are considered in the API.
     */
    static final boolean UNRESTRICTED = Boolean.getBoolean(SYSTEM_PROPERTY);

    @SuppressWarnings("unchecked")
    static CompletionStage<Object> toCompletionStage(Object result) {
        // Note that we intentionally use "instanceof" to test interfaces as the last resort in order to mitigate the "type pollution"
        // See https://github.com/RedHatPerf/type-pollution-agent for more information
        if (result instanceof CompletableFuture) {
            return (CompletableFuture<Object>) result;
        } else if (result instanceof CompletedStage) {
            return (CompletedStage<Object>) result;
        } else if (result instanceof AbstractUni) {
            return ((AbstractUni<Object>) result).subscribeAsCompletionStage();
        } else if (UNRESTRICTED && result instanceof CompletionStage) {
            return (CompletionStage<Object>) result;
        }
        return CompletedStage.of(result);
    }
}
