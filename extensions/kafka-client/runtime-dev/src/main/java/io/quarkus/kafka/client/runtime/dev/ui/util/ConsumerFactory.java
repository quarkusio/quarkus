package io.quarkus.kafka.client.runtime.dev.ui.util;

import java.util.*;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;

public class ConsumerFactory {

    public static Consumer<Bytes, Bytes> createConsumer(String topicName, Integer requestedPartition,
            Map<String, Object> commonConfig) {
        return createConsumer(List.of(new TopicPartition(topicName, requestedPartition)), commonConfig);
    }

    // We must create a new instance per request, as we might have multiple windows open, each with different pagination, filter and thus different cursor.
    public static Consumer<Bytes, Bytes> createConsumer(Collection<TopicPartition> requestedPartitions,
            Map<String, Object> commonConfig) {
        Map<String, Object> config = new HashMap<>(commonConfig);
        //TODO: make generic?
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);

        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-ui-" + UUID.randomUUID());

        // For pagination, we require manual management of offset pointer.
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        var consumer = new KafkaConsumer<Bytes, Bytes>(config);
        consumer.assign(requestedPartitions);
        return consumer;
    }

}
