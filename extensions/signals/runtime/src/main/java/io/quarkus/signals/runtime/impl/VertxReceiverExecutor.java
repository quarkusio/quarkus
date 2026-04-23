package io.quarkus.signals.runtime.impl;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

@Singleton
public class VertxReceiverExecutor implements ReceiverExecutor {

    private static final Logger LOG = Logger.getLogger(VertxReceiverExecutor.class);

    @Inject
    Vertx vertx;

    @Override
    public boolean supportsExecutionModel(ExecutionModel val) {
        return true;
    }

    @Override
    public <SIGNAL, RESPONSE> Uni<RESPONSE> execute(Receiver<SIGNAL, RESPONSE> receiver, SignalContext<SIGNAL> signalContext) {
        LOG.debugf("Notify %s [signal=%s, emission=%s]", receiver, signalContext.signalType(),
                signalContext.emissionType());
        Context context = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        VertxContextSafetyToggle.setContextSafe(context, true);
        Promise<RESPONSE> promise = Promise.promise();
        Future<RESPONSE> future = promise.future();
        context.runOnContext(new Handler<Void>() {
            @Override
            public void handle(Void v) {
                execute(context, promise, receiver.executionModel(), new Callable<Uni<RESPONSE>>() {
                    @Override
                    public Uni<RESPONSE> call() throws Exception {
                        return receiver.notify(signalContext);
                    }
                });
            }
        });
        return UniHelper.toUni(future);
    }

    protected <RESULT> void execute(Context context, Promise<RESULT> result, ExecutionModel executionModel,
            Callable<Uni<RESULT>> action) {
        if (executionModel == ExecutionModel.VIRTUAL_THREAD) {
            VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        action.call().subscribe().with(result::complete, result::fail);
                    } catch (Throwable e) {
                        result.fail(e);
                    }
                }
            });
        } else if (executionModel == ExecutionModel.BLOCKING) {
            context.executeBlocking(new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        action.call().subscribe().with(result::complete, result::fail);
                    } catch (Throwable e) {
                        result.fail(e);
                    }
                    return null;
                }
            }, false);
        } else {
            try {
                action.call().subscribe().with(result::complete, result::fail);
            } catch (Throwable e) {
                result.fail(e);
            }
        }
    }

}
