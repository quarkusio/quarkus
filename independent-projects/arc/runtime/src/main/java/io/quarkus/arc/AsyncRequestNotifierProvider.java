package io.quarkus.arc;

import java.util.concurrent.CompletionStage;

/**
 * Implement this service if you participate in asynchronous requests and can provide
 * information about completion/exception about it. All such providers will be combined
 * in order to determine completion/exception even if only one provider raises an exception
 * that is invisible to the other: we will merge completions and expose any exception in
 * {@link ArcContainer#getAsyncRequestNotifier()}.
 */
public interface AsyncRequestNotifierProvider {

    /**
     * @return a CompletionStage that gets completed when the current async request is completed
     *         or results in an exception.
     */
    CompletionStage<Void> getAsyncRequestNotifier();

}
