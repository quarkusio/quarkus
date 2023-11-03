package io.quarkus.vertx;

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
        Context context = getContext();
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
        Context context = getContext();
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

    private static Context getContext() {
        Context context = Vertx.currentContext();
        if (context == null) {
            Vertx vertx = VertxCoreRecorder.getVertx().get();
            context = VertxContext.getOrCreateDuplicatedContext(vertx);
        } else {
            // Executed on a vertx thread...
            if (Context.isOnEventLoopThread()) {
                throw new IllegalStateException("VertxContextSupport#subscribeAndAwait() must not be called on an event loop!");
            }
            context = VertxContext.getOrCreateDuplicatedContext(context);
        }
        return context;
    }

}
