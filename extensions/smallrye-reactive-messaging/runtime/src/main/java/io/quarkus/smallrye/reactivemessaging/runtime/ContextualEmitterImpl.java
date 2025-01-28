package io.quarkus.smallrye.reactivemessaging.runtime;

import static io.smallrye.reactive.messaging.providers.i18n.ProviderExceptions.ex;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.providers.extension.AbstractEmitter;
import io.smallrye.reactive.messaging.providers.locals.LocalContextMetadata;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class ContextualEmitterImpl<T> extends AbstractEmitter<T> implements ContextualEmitter<T> {

    public ContextualEmitterImpl(EmitterConfiguration configuration, long defaultBufferSize) {
        super(configuration, defaultBufferSize);
    }

    @Override
    public void sendAndAwait(T payload) {
        sendMessage(Message.of(payload)).await().indefinitely();
    }

    @Override
    public Uni<Void> send(T payload) {
        return sendMessage(Message.of(payload));
    }

    @Override
    public <M extends Message<? extends T>> void sendMessageAndAwait(M msg) {
        sendMessage(msg).await().indefinitely();
    }

    @Override
    @CheckReturnValue
    public <M extends Message<? extends T>> Uni<Void> sendMessage(M msg) {
        if (msg == null) {
            throw ex.illegalArgumentForNullValue();
        }

        // If we are running on a Vert.x context, we need to capture the context to switch back during the emission.
        Context context = Vertx.currentContext();
        Uni<Void> uni = Uni.createFrom().emitter(e -> {
            try {
                Message<? extends T> message = msg;
                if (VertxContext.isDuplicatedContext(context)) {
                    message = message.addMetadata(new LocalContextMetadata(context));
                }
                emit(message.withAck(() -> {
                    e.complete(null);
                    return msg.ack();
                })
                        .withNack(t -> {
                            e.fail(t);
                            return msg.nack(t);
                        }));
            } catch (Exception t) {
                // Capture synchronous exception and nack the message.
                msg.nack(t);
                throw t;
            }
        });
        if (context != null) {
            uni = uni.emitOn(runnable -> context.runOnContext(x -> runnable.run()));
        }
        return uni;
    }

}
