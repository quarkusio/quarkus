package io.quarkus.arc.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class InvokerCleanupTasks implements Consumer<Runnable> {
    private static final Logger LOG = Logger.getLogger(InvokerCleanupTasks.class);

    private final Set<Runnable> finishTasks = ConcurrentHashMap.newKeySet();

    @Override
    public void accept(Runnable task) {
        if (task != null) {
            finishTasks.add(task);
        }
    }

    // the generated invokers rely on this method not throwing
    public void finish() {
        for (Runnable task : finishTasks) {
            try {
                task.run();
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.error("Exception thrown by an invoker cleanup task", e);
                } else {
                    LOG.error("Exception thrown by an invoker cleanup task: " + e);
                }
            }
        }
        finishTasks.clear();
    }

    // ---

    public static <T> CompletionStage<T> deferRelease(CreationalContext<?> cc, CompletionStage<T> completionStage) {
        CompletableFuture<T> result = new CompletableFuture<>();
        completionStage.whenComplete((value, error) -> {
            cc.release();
            if (error == null) {
                result.complete(value);
            } else {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    public static <T> Uni<T> deferRelease(CreationalContext<?> cc, Uni<T> uni) {
        return uni.onTermination().invoke(cc::release);
    }

    public static <T> Multi<T> deferRelease(CreationalContext<?> cc, Multi<T> multi) {
        return multi.onTermination().invoke(cc::release);
    }
}
