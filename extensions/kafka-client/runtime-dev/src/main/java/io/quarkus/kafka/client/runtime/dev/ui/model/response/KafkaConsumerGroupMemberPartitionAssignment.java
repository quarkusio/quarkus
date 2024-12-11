package io.quarkus.kafka.client.runtime.dev.ui.model.response;

public class KafkaConsumerGroupMemberPartitionAssignment {

    private int partition;
    private String topic;
    private long lag;

    public KafkaConsumerGroupMemberPartitionAssignment() {
    }

    public KafkaConsumerGroupMemberPartitionAssignment(int partition, String topic, long lag) {
        this.partition = partition;
        this.topic = topic;
        this.lag = lag;
    }

    public int getPartition() {
        return partition;
    }

    public String getTopic() {
        return topic;
    }

    public long getLag() {
        return lag;
    }
}
