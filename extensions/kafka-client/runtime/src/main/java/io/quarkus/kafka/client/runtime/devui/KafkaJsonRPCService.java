package io.quarkus.kafka.client.runtime.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.ui.KafkaUiUtils;
import io.quarkus.kafka.client.runtime.ui.model.Order;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaCreateTopicRequest;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaMessageCreateRequest;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaMessagesRequest;
import io.quarkus.kafka.client.runtime.ui.model.request.KafkaOffsetRequest;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaAclInfo;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaInfo;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaMessagePage;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaTopic;

public class KafkaJsonRPCService {

    @Inject
    KafkaUiUtils kafkaUiUtils;

    @Inject
    KafkaAdminClient kafkaAdminClient;

    public List<KafkaTopic> getTopics() throws InterruptedException, ExecutionException {
        return kafkaUiUtils.getTopics();
    }

    public List<KafkaTopic> createTopic(String topicName, int partitions, int replications)
            throws InterruptedException, ExecutionException {

        KafkaCreateTopicRequest createTopicRequest = new KafkaCreateTopicRequest(topicName, partitions, (short) replications);
        boolean created = kafkaAdminClient.createTopic(createTopicRequest);
        if (created) {
            return kafkaUiUtils.getTopics();
        }
        throw new RuntimeException("Topic [" + topicName + "] not created");
    }

    public List<KafkaTopic> deleteTopic(String topicName) throws InterruptedException, ExecutionException {
        boolean deleted = kafkaAdminClient.deleteTopic(topicName);
        if (deleted) {
            return kafkaUiUtils.getTopics();
        }
        throw new RuntimeException("Topic [" + topicName + "] not deleted");
    }

    public KafkaMessagePage topicMessages(String topicName) throws ExecutionException, InterruptedException {
        List<Integer> partitions = getPartitions(topicName);
        KafkaOffsetRequest offsetRequest = new KafkaOffsetRequest(topicName, partitions, Order.NEW_FIRST);
        Map<Integer, Long> offset = kafkaUiUtils.getOffset(offsetRequest);
        KafkaMessagesRequest request = new KafkaMessagesRequest(topicName, Order.NEW_FIRST, 20, offset);
        return kafkaUiUtils.getMessages(request);
    }

    public KafkaMessagePage createMessage(String topicName, Integer partition, String key, String value,
            Map<String, String> headers)
            throws ExecutionException, InterruptedException {

        if (partition < 0)
            partition = null;

        KafkaMessageCreateRequest request = new KafkaMessageCreateRequest(topicName, partition, value, key, headers);

        kafkaUiUtils.createMessage(request);

        return topicMessages(topicName);
    }

    public List<Integer> getPartitions(String topicName) throws ExecutionException, InterruptedException {
        return new ArrayList<>(kafkaUiUtils.partitions(topicName));
    }

    public KafkaInfo getInfo() throws ExecutionException, InterruptedException {
        return kafkaUiUtils.getKafkaInfo();
    }

    public KafkaAclInfo getAclInfo() throws InterruptedException, ExecutionException {
        return kafkaUiUtils.getAclInfo();
    }

}
