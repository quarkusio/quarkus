package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.Collection;

public class KafkaConsumerGroupMember {
    private String memberId;
    private String clientId;
    private String host;

    private Collection<KafkaConsumerGroupMemberPartitionAssignment> partitions;

    public KafkaConsumerGroupMember() {
    }

    public KafkaConsumerGroupMember(String memberId, String clientId, String host,
            Collection<KafkaConsumerGroupMemberPartitionAssignment> partitions) {
        this.memberId = memberId;
        this.clientId = clientId;
        this.host = host;
        this.partitions = partitions;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getHost() {
        return host;
    }

    public Collection<KafkaConsumerGroupMemberPartitionAssignment> getPartitions() {
        return partitions;
    }
}
