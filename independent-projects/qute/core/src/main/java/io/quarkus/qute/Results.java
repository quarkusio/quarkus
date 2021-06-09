package io.quarkus.qute;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Result constants.
 */
public final class Results {

    public static final CompletionStage<Object> NOT_FOUND = CompletableFuture.completedFuture(Result.NOT_FOUND);
    public static final CompletableFuture<Object> FALSE = CompletableFuture.completedFuture(false);
    public static final CompletableFuture<Object> TRUE = CompletableFuture.completedFuture(true);
    public static final CompletableFuture<Object> NULL = CompletableFuture.completedFuture(null);

    private Results() {
    }

    public enum Result {

        NOT_FOUND;

        @Override
        public String toString() {
            return "NOT_FOUND";
        }
    }

}
