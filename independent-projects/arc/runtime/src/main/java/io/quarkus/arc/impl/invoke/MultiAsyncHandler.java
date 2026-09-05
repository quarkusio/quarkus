package io.quarkus.arc.impl.invoke;

import jakarta.enterprise.invoke.AsyncHandler;

import io.smallrye.mutiny.Multi;

public class MultiAsyncHandler<T> implements AsyncHandler.ReturnType<Multi<T>> {
    @Override
    public Multi<T> transform(Multi<T> original, Runnable completion) {
        return original.onTermination().invoke(completion);
    }
}
