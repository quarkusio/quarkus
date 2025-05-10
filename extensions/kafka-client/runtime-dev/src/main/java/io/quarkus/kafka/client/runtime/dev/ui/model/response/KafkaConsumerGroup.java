package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.Collection;

public class KafkaConsumerGroup {
    private String name;
    private String state;
    private String coordinatorHost;
    private int coordinatorId;
    // The assignment strategy
    private String protocol;
    private long lag;
    private Collection<KafkaConsumerGroupMember> members;

    public KafkaConsumerGroup() {
    }

    public KafkaConsumerGroup(String name, String state, String coordinatorHost, int coordinatorId, String protocol, long lag,
            Collection<KafkaConsumerGroupMember> members) {
        this.name = name;
        this.state = state;
        this.coordinatorHost = coordinatorHost;
        this.coordinatorId = coordinatorId;
        this.protocol = protocol;
        this.lag = lag;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public String getCoordinatorHost() {
        return coordinatorHost;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public String getProtocol() {
        return protocol;
    }

    public long getLag() {
        return lag;
    }

    public Collection<KafkaConsumerGroupMember> getMembers() {
        return members;
    }
}
