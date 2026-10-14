package io.quarkus.signals.runtime.impl;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.signals.Receivers.ExecutionModel;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

@Singleton
public class DefaultBlockingReceiverExecutor implements ReceiverExecutor {

    private static final Logger LOG = Logger.getLogger(DefaultBlockingReceiverExecutor.class);

    private final ExecutorService executorService;

    private final ConcurrencyLimiter blockingLimiter;

    DefaultBlockingReceiverExecutor(ExecutorService executorService, SignalsRuntimeConfig config) {
        this.executorService = executorService;
        int limit = config.receivers().blockingConcurrencyLimit().orElse(-1);
        this.blockingLimiter = limit > 0 ? new ConcurrencyLimiter(limit) : null;
    }

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

        Uni<RESPONSE> work = Uni.createFrom().deferred(new Supplier<Uni<? extends RESPONSE>>() {
            @Override
            public Uni<? extends RESPONSE> get() {
                return receiver.notify(context);
            }
        }).runSubscriptionOn(executorService);

        ConcurrencyLimiter limiter = blockingLimiter;
        if (limiter != null) {
            return Uni.createFrom().<Void> emitter(new Consumer<UniEmitter<? super Void>>() {
                @Override
                public void accept(UniEmitter<? super Void> em) {
                    limiter.run(new Runnable() {
                        @Override
                        public void run() {
                            em.complete(null);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t) {
                            em.fail(t);
                        }
                    });
                }
            }).replaceWith(work.eventually(limiter::complete));
        }
        return work;
    }

}
