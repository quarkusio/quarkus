package io.quarkus.kafka.client.runtime.dev.ui.model.request;

import java.util.List;

import io.quarkus.kafka.client.runtime.dev.ui.model.Order;

public class KafkaOffsetRequest {
    private String topicName;
    private List<Integer> requestedPartitions;
    private Order order;

    public KafkaOffsetRequest() {
    }

    public KafkaOffsetRequest(String topicName, List<Integer> requestedPartitions, Order order) {
        this.topicName = topicName;
        this.requestedPartitions = requestedPartitions;
        this.order = order;
    }

    public String getTopicName() {
        return topicName;
    }

    public List<Integer> getRequestedPartitions() {
        return requestedPartitions;
    }

    public Order getOrder() {
        return order;
    }
}
