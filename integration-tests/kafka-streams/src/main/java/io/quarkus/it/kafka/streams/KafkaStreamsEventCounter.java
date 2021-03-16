package io.quarkus.it.kafka.streams;

import java.util.concurrent.atomic.LongAdder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.kafka.streams.KafkaStreams;

@ApplicationScoped
public class KafkaStreamsEventCounter {

    LongAdder eventCount = new LongAdder();

    void onKafkaStreamsEvent(@Observes KafkaStreams kafkaStreams) {
        eventCount.increment();
    }

    public int getEventCount() {
        return eventCount.intValue();
    }
}
