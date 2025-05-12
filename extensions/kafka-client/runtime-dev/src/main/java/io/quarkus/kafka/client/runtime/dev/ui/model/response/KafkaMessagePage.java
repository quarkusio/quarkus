package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.Collection;
import java.util.Map;

public class KafkaMessagePage {
    private final Map<Integer, Long> nextOffsets;
    private final Collection<KafkaMessage> messages;

    public KafkaMessagePage(Map<Integer, Long> nextOffsets, Collection<KafkaMessage> messages) {
        this.nextOffsets = nextOffsets;
        this.messages = messages;
    }

    public Map<Integer, Long> getNextOffsets() {
        return nextOffsets;
    }

    public Collection<KafkaMessage> getMessages() {
        return messages;
    }
}
