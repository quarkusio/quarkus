package io.quarkus.kafka.client.runtime.dev.ui.model.response;

public class KafkaNode {
    private String host;
    private int port;
    private String id;

    public KafkaNode() {
    }

    public KafkaNode(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String asFullNodeName() {
        return host + ":" + port;
    }
}
