package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.entitylistener;

import java.lang.annotation.Annotation;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class EntityWithListenerThroughInstanceMethods {
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

    public void preUpdate() {
        receiveEvent(PreUpdate.class);
    }

    public void postUpdate() {
        receiveEvent(PostUpdate.class);
    }

    public void prePersist() {
        receiveEvent(PrePersist.class);
    }

    public void postPersist() {
        receiveEvent(PostPersist.class);
    }

    public void preRemove() {
        receiveEvent(PreRemove.class);
    }

    public void postRemove() {
        receiveEvent(PostRemove.class);
    }

    public void postLoad() {
        receiveEvent(PostLoad.class);
    }

    private void receiveEvent(Class<? extends Annotation> eventType) {
        String ref = this.toString();
        ReceivedEvent.add(ref, new ReceivedEvent(eventType, ref));
    }
}
