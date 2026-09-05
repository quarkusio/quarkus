package io.quarkus.arc.impl.invoke;

import jakarta.enterprise.invoke.AsyncHandler;

import io.smallrye.mutiny.Uni;

public class UniAsyncHandler<T> implements AsyncHandler.ReturnType<Uni<T>> {
    @Override
    public Uni<T> transform(Uni<T> original, Runnable completion) {
        return original.onTermination().invoke(completion);
    }
}
