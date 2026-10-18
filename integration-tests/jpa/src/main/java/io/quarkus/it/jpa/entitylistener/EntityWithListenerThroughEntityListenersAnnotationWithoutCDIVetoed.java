package io.quarkus.it.jpa.entitylistener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

@Entity
@EntityListeners(MyListenerNotRequiringCdiVetoed.class)
public class EntityWithListenerThroughEntityListenersAnnotationWithoutCDIVetoed {
    @Id
    private Integer id;

    private String text;

    @Override
    public String toString() {
        return ReceivedEvent.objectRef(EntityWithListenerThroughEntityListenersAnnotationWithoutCDIVetoed.class, id);
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
}
