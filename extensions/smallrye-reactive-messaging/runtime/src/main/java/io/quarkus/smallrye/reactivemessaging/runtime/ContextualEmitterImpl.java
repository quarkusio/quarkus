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
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.providers.extension.AbstractEmitter;
import io.smallrye.reactive.messaging.providers.i18n.ProviderLogging;
import io.smallrye.reactive.messaging.providers.locals.ContextAwareMessage;
import io.smallrye.reactive.messaging.providers.locals.LocalContextMetadata;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public class ContextualEmitterImpl<T> extends AbstractEmitter<T> implements ContextualEmitter<T> {

    public ContextualEmitterImpl(EmitterConfiguration configuration, long defaultBufferSize) {
        super(configuration, defaultBufferSize);
    }

    @Override
    public void sendAndAwait(T payload) {
        sendMessage(Message.of(payload)).await().indefinitely();
    }

    @Override
    public Cancellable sendAndForget(T payload) {
        return send(payload).subscribe().with(x -> {
            // Do nothing.
        }, ProviderLogging.log::failureEmittingMessage);
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
    public <M extends Message<? extends T>> Cancellable sendMessageAndForget(M msg) {
        return sendMessage(msg).subscribe().with(x -> {
            // Do nothing.
        }, ProviderLogging.log::failureEmittingMessage);
    }

    @Override
    @CheckReturnValue
    public <M extends Message<? extends T>> Uni<Void> sendMessage(M msg) {
        if (msg == null) {
            throw ex.illegalArgumentForNullValue();
        }

        // If we are running on a Vert.x context, we need to capture the context to switch back
        // during the emission.
        Context context = Vertx.currentContext();
        // context propagation capture and duplicate the context
        var msgUni = Uni.createFrom().item(() -> createContextualMessage((Message<? extends T>) msg, context));
        if (context != null) {
            msgUni = msgUni.emitOn(r -> context.runOnContext(x -> r.run()));
        }
        // emit the message, skip context propagation as it is unnecessary here
        Uni<Void> uni = transformToUni(msgUni, message -> ContextualEmitterImpl.emitter(e -> {
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
        }));
        // switch back to the caller context
        if (context != null) {
            return uni.emitOn(r -> context.runOnContext(x -> r.run()));
        } else {
            return uni;
        }
    }

    private static <T, M extends Message<T>> Message<T> createContextualMessage(M msg, Context context) {
        if (context == null) {
            // No context, return the message with a new context as is.
            return ContextAwareMessage.withContextMetadata(msg);
        } else {
            // create new context and copy local data from previous context
            ContextInternal internal = (ContextInternal) context;
            ContextInternal newCtx = internal.duplicate();
            newCtx.localContextData().putAll(internal.localContextData());
            return msg.addMetadata(new LocalContextMetadata(newCtx));
        }
    }

    public static <T> Uni<T> emitter(Consumer<UniEmitter<? super T>> emitter) {
        return Infrastructure.onUniCreation(new UniCreateWithEmitter<>(emitter));
    }

    public static <T, R> Uni<R> transformToUni(Uni<T> upstream, Function<? super T, Uni<? extends R>> mapper) {
        return Infrastructure.onUniCreation(new UniOnItemTransformToUni<>(upstream, mapper));
    }

}
