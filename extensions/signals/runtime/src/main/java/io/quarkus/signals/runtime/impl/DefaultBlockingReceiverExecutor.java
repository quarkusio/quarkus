package io.quarkus.signals.runtime.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.DefaultBean;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.smallrye.mutiny.Uni;

@DefaultBean
@Singleton
public class DefaultBlockingReceiverExecutor implements ReceiverExecutor {

    private static final Logger LOG = Logger.getLogger(DefaultBlockingReceiverExecutor.class);

    @Inject
    ExecutorService executorService;

    @Override
    public boolean supportsExecutionModel(ExecutionModel val) {
        return val == ExecutionModel.BLOCKING;
    }

    @Override
    public <SIGNAL, RESPONSE> Uni<RESPONSE> execute(Receiver<SIGNAL, RESPONSE> receiver, SignalContext<SIGNAL> context) {
        ExecutionModel executionModel = receiver.executionModel();
        if (!supportsExecutionModel(executionModel)) {
            throw new IllegalStateException(
                    "The execution model %s of %s is not supported".formatted(executionModel, receiver));
        }
        LOG.debugf("Notify %s [signal=%s, emission=%s]", receiver, context.signalType(),
                context.emissionType());
        CompletableFuture<RESPONSE> ret = execute(executionModel, new Callable<Uni<RESPONSE>>() {
            @Override
            public Uni<RESPONSE> call() throws Exception {
                return receiver.notify(context);
            }
        });
        return Uni.createFrom().completionStage(ret);
    }

    protected <RESULT> CompletableFuture<RESULT> execute(ExecutionModel executionModel, Callable<Uni<RESULT>> action) {
        CompletableFuture<RESULT> ret = new CompletableFuture<>();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    action.call().subscribe().with(ret::complete, ret::completeExceptionally);
                } catch (Throwable e) {
                    ret.completeExceptionally(e);
                }
            }
        });
        return ret;
    }

}
