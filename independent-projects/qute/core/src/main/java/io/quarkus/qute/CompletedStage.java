package io.quarkus.qute;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Internal representation of a completed computation, an optimized replacement for
 * {@link CompletableFuture#completedFuture(Object)}.
 * <p>
 * Note that this is not a full implementation of {@link CompletionStage} - it just throws {@link UnsupportedOperationException}
 * for methods that are not used internally. Therefore, it should not be used outside the Qute API.
 */
public final class CompletedStage<T> implements CompletionStage<T>, Supplier<T> {

    static final CompletedStage<Void> VOID = new CompletedStage<>(null, null);

    public static <T> CompletedStage<T> of(T result) {
        return new CompletedStage<T>(result, null);
    }

    public static <T> CompletedStage<T> failure(Throwable t) {
        return new CompletedStage<T>(null, t);
    }

    private final T result;
    private final Throwable exception;

    private CompletedStage(T result, Throwable exception) {
        this.result = result;
        this.exception = exception;
    }

    public T get() {
        return result;
    }

    @Override
    public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
        Objects.requireNonNull(fn);
        if (exception == null) {
            final U u;
            try {
                u = fn.apply(this.result);
            } catch (Throwable e) {
                return new CompletedStage<>(null, e);
            }
            return new CompletedStage<>(u, null);
        }
        return new CompletedStage<>(null, exception);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (exception == null) {
            try {
                action.accept(this.result);
            } catch (Throwable e) {
                return new CompletedStage<>(null, e);
            }
            return VOID;
        }
        return new CompletedStage<>(null, exception);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> thenRun(Runnable action) {
        Objects.requireNonNull(action);
        if (exception == null) {
            try {
                action.run();
            } catch (final Throwable e) {
                return new CompletedStage<>(null, e);
            }
            return VOID;
        }
        return new CompletedStage<>(null, exception);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        Objects.requireNonNull(fn);
        return other.thenApply(
                exception == null ? u -> fn.apply(this.result, u) : u -> {
                    throw CompletedStage.wrap(exception);
                });
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        if (exception == null) {
            try {
                return Objects.requireNonNull(fn).apply(result);
            } catch (final Throwable e) {
                return new CompletedStage<>(null, e);
            }
        }
        return new CompletedStage<>(null, exception);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        try {
            Objects.requireNonNull(action).accept(result, exception);
        } catch (Throwable e) {
            if (exception == null) {
                return new CompletedStage<>(null, e);
            }
        }
        return this;
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
        Objects.requireNonNull(fn);
        if (exception == null) {
            return this;
        }
        try {
            return CompletedStage.of(fn.apply(this.exception));
        } catch (final Throwable e) {
            return CompletedStage.failure(e);
        }
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        if (exception == null) {
            return CompletableFuture.completedFuture(result);
        }
        CompletableFuture<T> ret = new CompletableFuture<>();
        ret.completeExceptionally(exception);
        return ret;
    }

    private static <T> CompletionException wrap(final Throwable e) {
        return e instanceof CompletionException ? (CompletionException) e : new CompletionException(e);
    }

}
