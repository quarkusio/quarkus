package io.quarkus.smallrye.reactivemessaging.runtime;

import static io.smallrye.reactive.messaging.providers.i18n.ProviderExceptions.ex;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOnItemTransformToUni;
import io.smallrye.mutiny.operators.uni.builders.UniCreateWithEmitter;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.providers.extension.AbstractEmitter;
import io.smallrye.reactive.messaging.providers.locals.ContextAwareMessage;
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

        // If we are running on a Vert.x conmtext, we need to capture the context to switch back
        // during the emission.
        Context context = Vertx.currentContext();
        // context propagation capture and duplicate the context
        return Uni.createFrom().item(() -> ContextAwareMessage.withContextMetadata((Message<? extends T>) msg))
                .plug(uni -> {
                    // return the captured context early for destroying the state if some context type is cleared
                    if (context != null) {
                        return uni.emitOn(r -> context.runOnContext(x -> r.run()));
                    } else {
                        return uni;
                    }
                })
                // emit the message, skip context propagation as it is unnecessary here
                .plug(uni -> transformToUni(uni, message -> ContextualEmitterImpl.<Void> emitter(e -> {
                    try {
                        emit(message
                                .withAck(() -> {
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
                })))
                // switch back to the caller context
                .plug(uni -> {
                    if (context != null) {
                        return uni.emitOn(r -> context.runOnContext(x -> r.run()));
                    }
                    return uni;
                });
    }

    public static <T> Uni<T> emitter(Consumer<UniEmitter<? super T>> emitter) {
        return Infrastructure.onUniCreation(new UniCreateWithEmitter<>(emitter));
    }

    public static <T, R> Uni<R> transformToUni(Uni<T> upstream, Function<? super T, Uni<? extends R>> mapper) {
        return Infrastructure.onUniCreation(new UniOnItemTransformToUni<>(upstream, mapper));
    }

}
