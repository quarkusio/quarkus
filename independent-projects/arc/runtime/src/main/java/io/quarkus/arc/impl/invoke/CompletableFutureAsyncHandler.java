package io.quarkus.arc.impl.invoke;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.invoke.AsyncHandler;

public class CompletableFutureAsyncHandler<T> implements AsyncHandler.ReturnType<CompletableFuture<T>> {
    @Override
    public CompletableFuture<T> transform(CompletableFuture<T> original, Runnable completion) {
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
