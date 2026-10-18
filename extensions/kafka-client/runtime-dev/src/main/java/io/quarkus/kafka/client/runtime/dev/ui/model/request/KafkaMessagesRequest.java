package io.quarkus.kafka.client.runtime.dev.ui.model.request;

import java.util.Map;

import io.quarkus.kafka.client.runtime.dev.ui.model.Order;

public class KafkaMessagesRequest {
    private String topicName;
    private Order order;
    private int pageSize;
    private Integer pageNumber;

    private Map<Integer, Long> partitionOffset;

    public KafkaMessagesRequest() {
    }

    public KafkaMessagesRequest(String topicName, Order order, int pageSize, int pageNumber) {
        this.topicName = topicName;
        this.order = order;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    public KafkaMessagesRequest(String topicName, Order order, int pageSize, Map<Integer, Long> partitionOffset) {
        this.topicName = topicName;
        this.order = order;
        this.pageSize = pageSize;
        this.partitionOffset = partitionOffset;
    }

    public String getTopicName() {
        return topicName;
    }

    public Order getOrder() {
        return order;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public Map<Integer, Long> getPartitionOffset() {
        return partitionOffset;
    }
}
