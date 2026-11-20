package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.ArrayList;
import java.util.List;

public class KafkaAclInfo {
    private String clusterId;
    private String broker;
    private String aclOperations;
    private List<KafkaAclEntry> entries = new ArrayList<>();
    private String errorMessage;

    public KafkaAclInfo() {
    }

    public KafkaAclInfo(String clusterId, String broker, String aclOperations, List<KafkaAclEntry> entries) {
        this.clusterId = clusterId;
        this.broker = broker;
        this.aclOperations = aclOperations;
        this.entries = entries;
    }

    public KafkaAclInfo(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getBroker() {
        return broker;
    }

    public String getAclOperations() {
        return aclOperations;
    }

    public List<KafkaAclEntry> getEntries() {
        return entries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
