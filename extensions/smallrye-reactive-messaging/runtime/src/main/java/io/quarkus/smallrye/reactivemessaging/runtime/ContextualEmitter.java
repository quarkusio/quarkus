package io.quarkus.smallrye.reactivemessaging.runtime;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.EmitterType;

public interface ContextualEmitter<T> extends EmitterType {

    Uni<Void> send(T payload);

    void sendAndAwait(T payload);

    <M extends Message<? extends T>> Uni<Void> sendMessage(M msg);

    <M extends Message<? extends T>> void sendMessageAndAwait(M msg);
}
