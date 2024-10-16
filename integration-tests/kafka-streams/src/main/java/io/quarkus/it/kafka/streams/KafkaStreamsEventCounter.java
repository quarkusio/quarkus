package io.quarkus.it.kafka.streams;

import java.util.concurrent.atomic.LongAdder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;

@ApplicationScoped
public class KafkaStreamsEventCounter {

    LongAdder eventCount = new LongAdder();

    void onKafkaStreamsEvent(@Observes KafkaStreams kafkaStreams, StreamsConfig streamsConfig) {
        assert kafkaStreams.state() == KafkaStreams.State.CREATED;
        assert streamsConfig != null;
        eventCount.increment();
    }

    public int getEventCount() {
        return eventCount.intValue();
    }
}
