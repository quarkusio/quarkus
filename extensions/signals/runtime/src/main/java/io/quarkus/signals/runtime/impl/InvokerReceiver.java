package io.quarkus.signals.runtime.impl;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.invoke.Invoker;

import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.smallrye.mutiny.Uni;

public abstract class InvokerReceiver<SIGNAL, RESPONSE> implements Receiver<SIGNAL, RESPONSE> {

    private final Invoker<SIGNAL, RESPONSE> invoker;
    private final ReceiverInfo receiveInfo;

    public InvokerReceiver(Invoker<SIGNAL, RESPONSE> invoker, ReceiverInfo receiverInfo) {
        this.invoker = invoker;
        this.receiveInfo = receiverInfo;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Uni<RESPONSE> notify(SignalContext<SIGNAL> context) {
        Object[] args = new Object[receiveInfo.totalParams()];
        for (int i = 0; i < receiveInfo.totalParams(); i++) {
            if (i == receiveInfo.signalArgPosition()) {
                args[i] = receiveInfo.receiveContext() ? context : context.signal();
            } else {
                args[i] = null;
            }
        }
        try {
            Object ret = invoker.invoke(null, args);
            if (ret instanceof Uni uni) {
                return (Uni<RESPONSE>) uni;
            } else if (ret instanceof CompletionStage cs) {
                return (Uni<RESPONSE>) Uni.createFrom().completionStage(cs);
            } else {
                return (Uni<RESPONSE>) Uni.createFrom().item(ret);
            }
        } catch (Throwable e) {
            return Uni.createFrom().failure(e);
        }
    }

    public record ReceiverInfo(short signalArgPosition, boolean receiveContext, short totalParams) {
    }

    public String toString() {
        return "InvokerReceiver [invoker=" + invoker.getClass().getName() + "]";
    }

}
