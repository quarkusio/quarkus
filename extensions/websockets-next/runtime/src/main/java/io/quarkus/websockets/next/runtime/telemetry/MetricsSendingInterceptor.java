package io.quarkus.websockets.next.runtime.telemetry;

import java.nio.charset.StandardCharsets;

import io.micrometer.core.instrument.Counter;
import io.vertx.core.buffer.Buffer;

final class MetricsSendingInterceptor implements SendingInterceptor {

    private final Counter onMessageSentCounter;
    private final Counter onMessageSentBytesCounter;

    MetricsSendingInterceptor(Counter onMessageSentCounter, Counter onMessageSentBytesCounter) {
        this.onMessageSentCounter = onMessageSentCounter;
        this.onMessageSentBytesCounter = onMessageSentBytesCounter;
    }

    @Override
    public void onSend(String text) {
        onMessageSentCounter.increment();
        onMessageSentBytesCounter.increment(text.getBytes(StandardCharsets.UTF_8).length);
    }

    @Override
    public void onSend(Buffer message) {
        onMessageSentCounter.increment();
        onMessageSentBytesCounter.increment(message.getBytes().length);
    }
}
