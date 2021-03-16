package io.quarkus.reactivemessaging.http.sink.app;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

@ApplicationScoped
public class HttpEmitterWithOverflow {

    public static final int BUFFER_SIZE = 10;

    @Channel("my-http-sink")
    @OnOverflow(OnOverflow.Strategy.THROW_EXCEPTION)
    Emitter<Object> emitterThrowingOnOverflow;

    @Channel("my-http-sink2")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = BUFFER_SIZE)
    Emitter<Object> emitterBufferingOnOverflow;

    public CompletionStage<Void> emitAndThrowOnOverflow(int attemptNo) {
        return emitterThrowingOnOverflow.send(new Dto("attempt" + attemptNo));
    }

    public CompletionStage<Void> emitAndBufferOnOverflow(int attemptNo) {
        return emitterBufferingOnOverflow.send(new Dto("attempt" + attemptNo));
    }
}
