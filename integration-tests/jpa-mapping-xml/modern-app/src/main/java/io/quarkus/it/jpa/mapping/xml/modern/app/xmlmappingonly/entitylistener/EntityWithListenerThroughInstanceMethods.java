package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.entitylistener;

import java.lang.annotation.Annotation;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

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
