package io.quarkus.it.jpa.entitylistener;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import io.quarkus.arc.ClientProxy;

@ApplicationScoped
public class MyListenerRequiringCdi {
    private static final AtomicInteger instanceOrdinalSource = new AtomicInteger(0);

    @Inject
    MyCdiContext cdiContext;

    private final String ref;

    public MyListenerRequiringCdi() {
        int ordinal;
        if (!ClientProxy.class.isAssignableFrom(getClass())) { // Disregard CDI proxies extending this class
            ordinal = instanceOrdinalSource.getAndIncrement();
        } else {
            ordinal = -1;
        }
        this.ref = ReceivedEvent.objectRef(MyListenerRequiringCdi.class, ordinal);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        receiveEvent(PreUpdate.class, entity);
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        receiveEvent(PostUpdate.class, entity);
    }

    @PrePersist
    public void prePersist(Object entity) {
        receiveEvent(PrePersist.class, entity);
    }

    @PostPersist
    public void postPersist(Object entity) {
        receiveEvent(PostPersist.class, entity);
    }

    @PreRemove
    public void preRemove(Object entity) {
        receiveEvent(PreRemove.class, entity);
    }

    @PostRemove
    public void postRemove(Object entity) {
        receiveEvent(PostRemove.class, entity);
    }

    @PostLoad
    public void postLoad(Object entity) {
        receiveEvent(PostLoad.class, entity);
    }

    private void receiveEvent(Class<? extends Annotation> eventType, Object entity) {
        MyCdiContext.checkAvailable(cdiContext);
        ReceivedEvent.add(ref, new ReceivedEvent(eventType, entity.toString()));
    }
}
