package io.quarkus.reactivemessaging.websocket.sink.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

@SuppressWarnings("rawtypes")
@ApplicationScoped
public class WebSocketEmitter {

    public static final String BUFFER = "BUFFER";
    public static final String JSON_OBJECT = "JSON_OBJECT";
    public static final String JSON_ARRAY = "JSON_ARRAY";
    public static final String STRING = "STRING";

    @Inject
    @Channel("my-ws-sink")
    Emitter emitter;

    @Inject
    @Channel("ws-sink-with-serializer")
    Emitter emitterWithCustomSerializer;

    public void sendMessage(Message<?> message) {
        emitter.send(message);
    }

    public void sendMessageWithCustomSerializer(Message<String> message) {
        emitterWithCustomSerializer.send(message);
    }
}
