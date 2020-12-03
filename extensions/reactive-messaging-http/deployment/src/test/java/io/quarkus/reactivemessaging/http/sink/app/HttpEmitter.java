package io.quarkus.reactivemessaging.http.sink.app;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.vertx.core.buffer.Buffer;

@ApplicationScoped
public class HttpEmitter {

    @Channel("my-http-sink")
    Emitter<Object> emitter;

    @Channel("http-sink-with-path-param")
    Emitter<Object> emitterWithPathParam;

    @Channel("retrying-http-sink")
    Emitter<Object> retryingEmitter;

    @Incoming("custom-http-source")
    @Outgoing("custom-http-sink")
    String passThroughWithCustomSerializer(Buffer message) {
        return message.toString();
    }

    public <T> void emitMessageWithPathParam(Message<T> message) {
        emitterWithPathParam.send(message);
    }

    public CompletionStage<Void> retryingEmitObject(Object message) {
        return retryingEmitter.send(message);
    }

    public CompletionStage<Void> emitObject(Object message) {
        return emitter.send(message);
    }
}
