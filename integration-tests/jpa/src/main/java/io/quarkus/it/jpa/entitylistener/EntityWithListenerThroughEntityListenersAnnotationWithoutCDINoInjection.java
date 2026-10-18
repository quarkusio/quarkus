package io.quarkus.it.jpa.entitylistener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

@Entity
@EntityListeners(MyListenerNotRequiringCdiNoInjection.class)
public class EntityWithListenerThroughEntityListenersAnnotationWithoutCDINoInjection {
    @Id
    private Integer id;

    private String text;

    @Override
    public String toString() {
        return ReceivedEvent.objectRef(EntityWithListenerThroughEntityListenersAnnotationWithoutCDINoInjection.class, id);
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
