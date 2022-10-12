package io.quarkus.smallrye.reactivemessaging.kafka;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;

import org.apache.kafka.common.TopicPartition;

import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;

@MappedSuperclass
public class CheckpointEntity {

    public static <S extends CheckpointEntity> S from(ProcessingState<S> state, CheckpointEntityId entityId) {
        S stateState = state.getState();
        stateState.setOffset(state.getOffset());
        if (stateState.getId() == null) {
            stateState.setId(entityId);
        }
        return stateState;
    }

    public static TopicPartition topicPartition(CheckpointEntity entity) {
        if (entity == null) {
            return null;
        }
        CheckpointEntityId id = entity.getId();
        if (id == null) {
            return null;
        }
        return new TopicPartition(id.getTopic(), id.getPartition());
    }

    @EmbeddedId
    CheckpointEntityId id;

    @Column(name = "record_offset")
    public Long offset;

    public CheckpointEntity() {
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public CheckpointEntityId getId() {
        return id;
    }

    public void setId(CheckpointEntityId id) {
        this.id = id;
    }

}
