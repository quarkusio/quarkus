package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.ArrayList;
import java.util.List;

public class KafkaClusterInfo {
    private String id;
    private KafkaNode controller;
    private List<KafkaNode> nodes = new ArrayList<>();
    private String aclOperations;

    public KafkaClusterInfo() {
    }

    public KafkaClusterInfo(String id, KafkaNode controller, List<KafkaNode> nodes, String aclOperations) {
        this.id = id;
        this.controller = controller;
        this.nodes = nodes;
        this.aclOperations = aclOperations;
    }

    public String getId() {
        return id;
    }

    public KafkaNode getController() {
        return controller;
    }

    public List<KafkaNode> getNodes() {
        return nodes;
    }

    public String getAclOperations() {
        return aclOperations;
    }
}
