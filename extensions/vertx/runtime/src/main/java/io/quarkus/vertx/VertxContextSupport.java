package io.quarkus.vertx;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiSubscribe;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Provides utility methods to work with Vertx duplicated context.
 *
 * @see VertxContext
 */
public final class VertxContextSupport {

    private VertxContextSupport() {
    }

    /**
     * Subscribes to the supplied {@link Uni} on a Vertx duplicated context; blocks the current thread and waits for the result.
     * <p>
     * If it's necessary, the CDI request context is activated during execution of the asynchronous code.
     *
     * @param uniSupplier
     * @throws IllegalStateException If called on an event loop thread.
     */
    public static <T> T subscribeAndAwait(Supplier<Uni<T>> uniSupplier) throws Throwable {
        Context context = getContext(false);
        VertxContextSafetyToggle.setContextSafe(context, true);
        return Uni.createFrom().<T> emitter(e -> {
            context.runOnContext(new Handler<Void>() {

                @Override
                public void handle(Void event) {
                    ManagedContext requestContext = Arc.container().requestContext();
                    Runnable terminate = null;
                    if (!requestContext.isActive()) {
                        requestContext.activate();
                        terminate = requestContext::terminate;
                    }
                    try {
                        Uni<T> uni = uniSupplier.get();
                        if (terminate != null) {
                            uni = uni.onTermination().invoke(terminate);
                        }
                        uni.subscribe().with(e::complete, e::fail);
                    } catch (Throwable t) {
                        e.fail(t);
                    }
                }
            });
        }).await().indefinitely();
    }

    /**
     * Subscribes to the supplied {@link Multi} on a Vertx duplicated context; does not block the current thread.
     *
     * @param <T>
     * @param multiSupplier
     * @param subscribeConsumer
     */
    public static <T> void subscribe(Supplier<Multi<T>> multiSupplier, Consumer<MultiSubscribe<T>> subscribeConsumer) {
        Context context = getContext(false);
        VertxContextSafetyToggle.setContextSafe(context, true);
        context.runOnContext(new Handler<Void>() {

            @Override
            public void handle(Void event) {
                subscribeConsumer.accept(multiSupplier.get().subscribe());
            }
        });
    }

    /**
     * Subscribes to the supplied {@link Multi} on a Vertx duplicated context; does not block the current thread.
     *
     * @param <T>
     * @param multiSupplier
     * @param onItem
     */
    public static <T> void subscribeWith(Supplier<Multi<T>> multiSupplier, Consumer<? super T> onItem) {
        subscribe(multiSupplier, new Consumer<MultiSubscribe<T>>() {
            @Override
            public void accept(MultiSubscribe<T> ms) {
                ms.with(onItem);
            }
        });
    }

    /**
     * Executes the supplied blocking {@link Callable} on a Vertx duplicated context; does not block the current thread.
     * <p>
     * If necessary, the CDI request context is activated during execution of the blocking code.
     *
     * @param <T>
     * @param callable
     * @return the produced {@link Uni}
     * @see VertxContext#getOrCreateDuplicatedContext(Vertx)
     */
    public static <T> Uni<T> executeBlocking(Callable<T> callable) {
        Context context = getContext(true);
        return Uni.createFrom().completionStage(() -> {
            return context.executeBlocking(() -> {
                ManagedContext requestContext = Arc.container().requestContext();
                boolean terminate = requestContext.isActive() ? false : true;
                if (terminate) {
                    requestContext.activate();
                }
                try {
                    return callable.call();
                } finally {
                    if (terminate) {
                        requestContext.terminate();
                    }
                }
            }, false).toCompletionStage();
        });
    }

    private static Context getContext(boolean blocking) {
        Context context = Vertx.currentContext();
        if (context == null) {
            Vertx vertx = VertxCoreRecorder.getVertx().get();
            context = VertxContext.getOrCreateDuplicatedContext(vertx);
        } else {
            // Executed on a vertx thread...
            if (!blocking && Context.isOnEventLoopThread()) {
                throw new IllegalStateException("VertxContextSupport#subscribeAndAwait() must not be called on an event loop!");
            }
            context = VertxContext.getOrCreateDuplicatedContext(context);
        }
        return context;
    }

}
