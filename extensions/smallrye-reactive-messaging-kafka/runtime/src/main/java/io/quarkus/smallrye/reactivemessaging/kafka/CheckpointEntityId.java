package io.quarkus.smallrye.reactivemessaging.kafka;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.kafka.common.TopicPartition;

@Embeddable
public class CheckpointEntityId implements Serializable {
    private static final long serialVersionUID = -259817999246156947L;

    @Column(name = "consumer_group_id", insertable = false)
    private String consumerGroupId;
    private String topic;
    private int partition;

    public CheckpointEntityId() {
    }

    public CheckpointEntityId(String consumerGroupId, TopicPartition topicPartition) {
        this.consumerGroupId = consumerGroupId;
        this.topic = topicPartition.topic();
        this.partition = topicPartition.partition();
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CheckpointEntityId))
            return false;
        CheckpointEntityId that = (CheckpointEntityId) o;
        return partition == that.getPartition() && Objects.equals(consumerGroupId, that.getConsumerGroupId())
                && Objects.equals(topic, that.getTopic());
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerGroupId, topic, partition);
    }

    @Override
    public String toString() {
        return consumerGroupId + ':' + topic + ':' + partition;
    }
}
