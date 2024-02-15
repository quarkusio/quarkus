package io.quarkus.kafka.client.runtime.devui;

import static io.quarkus.kafka.client.runtime.devui.util.ConsumerFactory.createConsumer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.utils.Bytes;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.devui.model.Order;
import io.quarkus.kafka.client.runtime.devui.model.converter.KafkaModelConverter;
import io.quarkus.kafka.client.runtime.devui.model.request.KafkaMessageCreateRequest;
import io.quarkus.kafka.client.runtime.devui.model.response.KafkaMessagePage;
import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
public class KafkaTopicClient {
    // TODO: make configurable
    private static final int RETRIES = 3;

    @Inject
    KafkaAdminClient adminClient;

    KafkaModelConverter modelConverter = new KafkaModelConverter();

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    private Producer<Bytes, Bytes> createProducer() {
        Map<String, Object> config = new HashMap<>(this.config);

        config.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka-ui-producer-" + UUID.randomUUID());
        // TODO: make generic to support AVRO serializer
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, BytesSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BytesSerializer.class.getName());

        return new KafkaProducer<>(config);
    }

    /**
     * Reads the messages from particular topic. Offset for next page is returned within response.
     * The first/last page offset could be retrieved with
     * {@link KafkaTopicClient#getPagePartitionOffset(String, Collection, Order)}
     * method.
     *
     * @param topicName topic to read messages from
     * @param order ascending or descending. Defaults to descending (newest first)
     * @param partitionOffsets the offset for page to be read
     * @param pageSize size of read page
     * @return page of messages, matching requested filters
     */
    public KafkaMessagePage getTopicMessages(
            String topicName,
            Order order,
            Map<Integer, Long> partitionOffsets,
            int pageSize)
            throws ExecutionException, InterruptedException {
        assertParamsValid(pageSize, partitionOffsets);

        var requestedPartitions = partitionOffsets.keySet();
        assertRequestedPartitionsExist(topicName, requestedPartitions);
        if (order == null)
            order = Order.OLD_FIRST;

        var allPartitionsResult = getConsumerRecords(topicName, order, pageSize, requestedPartitions, partitionOffsets,
                pageSize);

        Comparator<ConsumerRecord<Bytes, Bytes>> comparator = Comparator.comparing(ConsumerRecord::timestamp);
        if (Order.NEW_FIRST == order)
            comparator = comparator.reversed();
        allPartitionsResult.sort(comparator);

        // We might have too many values. Throw away newer items, which don't fit into page.
        if (allPartitionsResult.size() > pageSize) {
            allPartitionsResult = allPartitionsResult.subList(0, pageSize);
        }

        var newOffsets = calculateNewPartitionOffset(partitionOffsets, allPartitionsResult, order, topicName);
        var convertedResult = allPartitionsResult.stream()
                .map(modelConverter::convert)
                .collect(Collectors.toList());
        return new KafkaMessagePage(newOffsets, convertedResult);
    }

    // Fail fast on wrong params, even before querying Kafka.
    private void assertParamsValid(int pageSize, Map<Integer, Long> partitionOffsets) {
        if (pageSize <= 0)
            throw new IllegalArgumentException("Page size must be > 0.");

        if (partitionOffsets == null || partitionOffsets.isEmpty())
            throw new IllegalArgumentException("Partition offset map must be specified.");

        for (var partitionOffset : partitionOffsets.entrySet()) {
            if (partitionOffset.getValue() < 0)
                throw new IllegalArgumentException(
                        "Partition offset must be > 0.");
        }
    }

    private ConsumerRecords<Bytes, Bytes> pollWhenReady(Consumer<Bytes, Bytes> consumer) {
        var attempts = 0;
        var pullDuration = Duration.of(100, ChronoUnit.MILLIS);
        var result = consumer.poll(pullDuration);

        while (result.isEmpty() && attempts < RETRIES) {
            result = consumer.poll(pullDuration);
            attempts++;
        }
        return result;
    }

    /*
     * FIXME: should consider compaction strategy, when our new offset not necessary = old + total records read, but some
     * records might be deleted, so we'll end up seeing duplicates on some pages.
     * Imagine this case:
     * - page size = 10
     * - 30 messages pushed, value is incremental 1 ... 30.
     * - message 10 gets removed, as message 15 has same key because of compaction
     * - we request page 1. it had offset 0. we return values [1, 2, 3, ..., 9, 11], total of 10. We get new offset for page 2 =
     * 0 + totalRecords = 10.
     * - we request page 2. we read starting from offset = 10. There is no message with that offset, but we see message 11 again
     * instead.
     */
    private Map<Integer, Long> calculateNewPartitionOffset(Map<Integer, Long> oldPartitionOffset,
            Collection<ConsumerRecord<Bytes, Bytes>> records, Order order, String topicName) {
        var newOffsets = records.stream().map(ConsumerRecord::partition)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        var newPartitionOffset = new HashMap<Integer, Long>();
        for (var partition : oldPartitionOffset.keySet()) {
            // We should add in case we seek for oldest and reduce for newest.
            var multiplier = Order.OLD_FIRST == order ? 1 : -1;

            // If new offset for partition is not there in the map - we didn't have records for that partition. So, just take the old offset.
            var newOffset = oldPartitionOffset.get(partition) + multiplier * newOffsets.getOrDefault(partition, 0L);
            newPartitionOffset.put(partition, newOffset);
        }
        return newPartitionOffset;
    }

    private long getPosition(String topicName, int partition, Order order) {
        try (var consumer = createConsumer(topicName, partition, this.config)) {
            var topicPartition = new TopicPartition(topicName, partition);
            if (Order.NEW_FIRST == order) {
                consumer.seekToEnd(List.of(topicPartition));
            } else {
                consumer.seekToBeginning(List.of(topicPartition));
            }
            return consumer.position(topicPartition);
        }
    }

    public Map<Integer, Long> getPagePartitionOffset(String topicName, Collection<Integer> requestedPartitions, Order order)
            throws ExecutionException, InterruptedException {
        assertRequestedPartitionsExist(topicName, requestedPartitions);

        var result = new HashMap<Integer, Long>();
        for (var requestedPartition : requestedPartitions) {
            var maxPosition = getPosition(topicName, requestedPartition, order);
            result.put(requestedPartition, maxPosition);
        }

        return result;
    }

    private List<ConsumerRecord<Bytes, Bytes>> getConsumerRecords(String topicName, Order order, int pageSize,
            Collection<Integer> requestedPartitions, Map<Integer, Long> start, int totalMessages) {
        List<ConsumerRecord<Bytes, Bytes>> allPartitionsResult = new ArrayList<>();

        // Requesting a full page from each partition and then filtering out redundant data. Thus, we'll ensure, we read data in historical order.
        for (var requestedPartition : requestedPartitions) {
            List<ConsumerRecord<Bytes, Bytes>> partitionResult = new ArrayList<>();
            var offset = start.get(requestedPartition);
            try (var consumer = createConsumer(topicName, requestedPartition, this.config)) {
                // Move pointer to currently read position. It might be different per partition, so requesting with offset per partition.
                var partition = new TopicPartition(topicName, requestedPartition);

                var seekedOffset = Order.OLD_FIRST == order ? offset : Long.max(offset - pageSize, 0);
                consumer.seek(partition, seekedOffset);

                var numberOfMessagesReadSoFar = 0;
                var keepOnReading = true;

                while (keepOnReading) {
                    var records = pollWhenReady(consumer);
                    if (records.isEmpty())
                        keepOnReading = false;

                    for (var record : records) {
                        numberOfMessagesReadSoFar++;
                        partitionResult.add(record);

                        if (numberOfMessagesReadSoFar >= totalMessages) {
                            keepOnReading = false;
                            break;
                        }
                    }
                }
                // We need to cut off result, if it was reset to 0, as we don't want see entries from old pages.
                if (Order.NEW_FIRST == order && seekedOffset == 0 && partitionResult.size() > offset.intValue()) {
                    partitionResult.sort(Comparator.comparing(ConsumerRecord::timestamp));
                    partitionResult = partitionResult.subList(0, offset.intValue());
                }

            }
            allPartitionsResult.addAll(partitionResult);
        }
        return allPartitionsResult;
    }

    private void assertRequestedPartitionsExist(String topicName, Collection<Integer> requestedPartitions)
            throws InterruptedException, ExecutionException {
        var topicPartitions = partitions(topicName);

        if (!new HashSet<>(topicPartitions).containsAll(requestedPartitions)) {
            throw new IllegalArgumentException(String.format(
                    "Requested messages from partition, that do not exist. Requested partitions: %s. Existing partitions: %s",
                    requestedPartitions, topicPartitions));
        }
    }

    public void createMessage(KafkaMessageCreateRequest request) {
        var record = new ProducerRecord<>(request.getTopic(), request.getPartition(),
                Bytes.wrap(request.getKey().getBytes(StandardCharsets.UTF_8)),
                Bytes.wrap(request.getValue().getBytes(StandardCharsets.UTF_8)));

        Optional.ofNullable(request.getHeaders())
                .orElseGet(Collections::emptyMap)
                .forEach((key, value) -> record.headers().add(
                        key,
                        Optional.ofNullable(value)
                                .map(v -> v.getBytes(StandardCharsets.UTF_8))
                                .orElse(null)));

        try (var producer = createProducer()) {
            producer.send(record);
        }
    }

    public List<Integer> partitions(String topicName) throws ExecutionException, InterruptedException {
        return adminClient.describeTopics(List.of(topicName))
                .values().stream()
                .reduce((a, b) -> {
                    throw new IllegalStateException(
                            "Requested info about single topic, but got result of multiple: " + a + ", " + b);
                })
                .orElseThrow(() -> new IllegalStateException(
                        "Requested info about a topic, but nothing found. Topic name: " + topicName))
                .partitions().stream()
                .map(TopicPartitionInfo::partition)
                .collect(Collectors.toList());
    }
}
