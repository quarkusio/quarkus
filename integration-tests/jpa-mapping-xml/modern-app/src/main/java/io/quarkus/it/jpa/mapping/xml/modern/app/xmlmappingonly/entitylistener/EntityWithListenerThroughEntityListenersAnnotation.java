package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.entitylistener;

public class EntityWithListenerThroughEntityListenersAnnotation {
    private Integer id;

    private String text;

    @Override
    public String toString() {
        return ReceivedEvent.objectRef(EntityWithListenerThroughEntityListenersAnnotation.class, id);
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
