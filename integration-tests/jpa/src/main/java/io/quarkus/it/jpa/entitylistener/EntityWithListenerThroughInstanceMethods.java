package io.quarkus.it.jpa.entitylistener;

import java.lang.annotation.Annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

@Entity
public class EntityWithListenerThroughInstanceMethods {
    @Id
    private Integer id;

    private String text;

    @Override
    public String toString() {
        return ReceivedEvent.objectRef(EntityWithListenerThroughInstanceMethods.class, id);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @PreUpdate
    public void preUpdate() {
        receiveEvent(PreUpdate.class);
    }

    @PostUpdate
    public void postUpdate() {
        receiveEvent(PostUpdate.class);
    }

    @PrePersist
    public void prePersist() {
        receiveEvent(PrePersist.class);
    }

    @PostPersist
    public void postPersist() {
        receiveEvent(PostPersist.class);
    }

    @PreRemove
    public void preRemove() {
        receiveEvent(PreRemove.class);
    }

    @PostRemove
    public void postRemove() {
        receiveEvent(PostRemove.class);
    }

    @PostLoad
    public void postLoad() {
        receiveEvent(PostLoad.class);
    }

    private void receiveEvent(Class<? extends Annotation> eventType) {
        String ref = this.toString();
        ReceivedEvent.add(ref, new ReceivedEvent(eventType, ref));
    }
}
