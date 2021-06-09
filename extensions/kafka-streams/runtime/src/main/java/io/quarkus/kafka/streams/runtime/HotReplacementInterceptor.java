package io.quarkus.kafka.streams.runtime;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public class HotReplacementInterceptor implements ConsumerInterceptor {

    private static volatile Runnable onMessage;

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public ConsumerRecords onConsume(ConsumerRecords records) {
        if (onMessage != null) {
            onMessage.run();
        }

        return records;
    }

    @Override
    public void onCommit(Map offsets) {
    }

    @Override
    public void close() {
    }

    public static void onMessage(Runnable handler) {
        onMessage = handler;
    }
}
