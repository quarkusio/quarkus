package io.quarkus.arc.impl.invoke;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.invoke.AsyncHandler;

public class CompletionStageAsyncHandler<T> implements AsyncHandler.ReturnType<CompletionStage<T>> {
    @Override
    public CompletionStage<T> transform(CompletionStage<T> original, Runnable completion) {
        CompletableFuture<T> result = new CompletableFuture<>();
        original.whenComplete((value, error) -> {
            completion.run();

            if (error == null) {
                result.complete(value);
            } else {
                result.completeExceptionally(error);
            }
        });
        return result;
    }
}
