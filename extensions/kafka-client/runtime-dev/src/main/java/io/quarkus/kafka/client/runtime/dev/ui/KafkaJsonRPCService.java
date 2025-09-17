package io.quarkus.kafka.client.runtime.dev.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.KafkaCreateTopicRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.Order;
import io.quarkus.kafka.client.runtime.dev.ui.model.request.KafkaMessageCreateRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.request.KafkaMessagesRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.request.KafkaOffsetRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaAclInfo;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaInfo;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessagePage;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaTopic;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class KafkaJsonRPCService {

    @Inject
    KafkaUiUtils kafkaUiUtils;

    @Inject
    KafkaAdminClient kafkaAdminClient;

    private final BroadcastProcessor<String> stateNotification = BroadcastProcessor.create(); // Send a notification that the state (topic/message) has changed
    private static final String TOPIC = "topic";
    private static final String MESSAGE = "message";

    public Multi<String> stateNotification() {
        return stateNotification;
    }

    @JsonRpcDescription("Get all the current Kafka topics")
    @DevMCPEnableByDefault
    public List<KafkaTopic> getTopics() throws InterruptedException, ExecutionException {
        return kafkaUiUtils.getTopics();
    }

    @JsonRpcDescription("Create a new Kafka topic")
    @DevMCPEnableByDefault
    public List<KafkaTopic> createTopic(@JsonRpcDescription("The Kafka topic name") final String topicName,
            @JsonRpcDescription("The number of partitions, example 1") final int partitions,
            @JsonRpcDescription("The number of replications, example 1") final int replications,
            @JsonRpcDescription("Other config in map format (key/value)") Map<String, String> configs)
            throws InterruptedException, ExecutionException {

        KafkaCreateTopicRequest createTopicRequest = new KafkaCreateTopicRequest(topicName, partitions, (short) replications,
                configs);
        boolean created = kafkaAdminClient.createTopic(createTopicRequest);
        if (created) {
            stateNotification.onNext(TOPIC);
            return kafkaUiUtils.getTopics();
        }
        throw new RuntimeException("Topic [" + topicName + "] not created");
    }

    @JsonRpcDescription("Delete an existing Kafka topic")
    @DevMCPEnableByDefault
    public List<KafkaTopic> deleteTopic(@JsonRpcDescription("The Kafka topic name") final String topicName)
            throws InterruptedException, ExecutionException {
        boolean deleted = kafkaAdminClient.deleteTopic(topicName);
        if (deleted) {
            stateNotification.onNext(TOPIC);
            return kafkaUiUtils.getTopics();
        }
        throw new RuntimeException("Topic [" + topicName + "] not deleted");
    }

    @JsonRpcDescription("Get all the current messages for a certain Kafka topics")
    @DevMCPEnableByDefault
    public KafkaMessagePage topicMessages(@JsonRpcDescription("The Kafka topic name") final String topicName)
            throws ExecutionException, InterruptedException {
        List<Integer> partitions = getPartitions(topicName);
        KafkaOffsetRequest offsetRequest = new KafkaOffsetRequest(topicName, partitions, Order.NEW_FIRST);
        Map<Integer, Long> offset = kafkaUiUtils.getOffset(offsetRequest);
        KafkaMessagesRequest request = new KafkaMessagesRequest(topicName, Order.NEW_FIRST, 20, offset);
        return kafkaUiUtils.getMessages(request);
    }

    @JsonRpcDescription("Create a new message on a specific Kafka topic")
    @DevMCPEnableByDefault
    public KafkaMessagePage createMessage(@JsonRpcDescription("The Kafka topic name") String topicName,
            @JsonRpcDescription("The partition number, example 1") Integer partition,
            @JsonRpcDescription("The message key") String key,
            @JsonRpcDescription("The message value") String value,
            @JsonRpcDescription("The message headers in map format (key/value)") Map<String, String> headers)
            throws ExecutionException, InterruptedException {

        if (partition < 0)
            partition = null;

        KafkaMessageCreateRequest request = new KafkaMessageCreateRequest(topicName, partition, value, key, headers);

        kafkaUiUtils.createMessage(request);
        stateNotification.onNext(MESSAGE);

        return topicMessages(topicName);
    }

    @JsonRpcDescription("Get the partitions for a specific Kafka topic")
    public List<Integer> getPartitions(@JsonRpcDescription("The Kafka topic name") final String topicName)
            throws ExecutionException, InterruptedException {
        return new ArrayList<>(kafkaUiUtils.partitions(topicName));
    }

    @JsonRpcDescription("Get all know information on the Kafka instance")
    @DevMCPEnableByDefault
    public KafkaInfo getInfo() throws ExecutionException, InterruptedException {
        return kafkaUiUtils.getKafkaInfo();
    }

    @JsonRpcDescription("Get all know information about the use access control lists for authorization in Kafka")
    public KafkaAclInfo getAclInfo() throws InterruptedException, ExecutionException {
        return kafkaUiUtils.getAclInfo();
    }

}
