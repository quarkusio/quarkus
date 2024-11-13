package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Offloads the execution of the scheduled task if necessary.
 *
 * @see ScheduledInvoker#isBlocking()
 * @see ScheduledInvoker#isRunningOnVirtualThread()
 */
public class OffloadingInvoker extends DelegateInvoker {

    private final Vertx vertx;

    public OffloadingInvoker(ScheduledInvoker delegate, Vertx vertx) {
        super(delegate);
        this.vertx = vertx;
    }

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        VertxContextSafetyToggle.setContextSafe(context, true);
        if (delegate.isBlocking()) {
            if (delegate.isRunningOnVirtualThread()) {
                // While counter-intuitive, we switch to a safe context, so that context is captured and attached
                // to the virtual thread.
                context.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                            @Override
                            public void run() {
                                invokeComplete(ret, execution);
                            }
                        });
                    }
                });
            } else {
                context.executeBlocking(new Callable<Void>() {
                    @Override
                    public Void call() {
                        invokeComplete(ret, execution);
                        return null;
                    }
                }, false);
            }
        } else {
            context.runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    invokeComplete(ret, execution);
                }
            });
        }
        return ret;
    }

}
