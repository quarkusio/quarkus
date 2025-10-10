package io.quarkus.it.jpa.entitylistener;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import io.quarkus.arc.ClientProxy;
import io.quarkus.it.jpa.util.BeanInstantiator;
import io.quarkus.it.jpa.util.MyCdiContext;

@Vetoed // We really don't want CDI for some reason.
public class MyListenerNotRequiringCdiVetoed {
    private static final AtomicInteger instanceOrdinalSource = new AtomicInteger(0);

    // This will always be null.
    // It's only here to check that one can force CDI not to be used.
    @Inject
    MyCdiContext cdiContext;

    private final String ref;

    private final BeanInstantiator beanInstantiator;

    public MyListenerNotRequiringCdiVetoed() {
        this.beanInstantiator = BeanInstantiator.fromCaller();
        int ordinal;
        if (!ClientProxy.class.isAssignableFrom(getClass())) { // Disregard CDI proxies extending this class
            ordinal = instanceOrdinalSource.getAndIncrement();
        } else {
            ordinal = -1;
        }
        this.ref = ReceivedEvent.objectRef(MyListenerNotRequiringCdiVetoed.class, ordinal);
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
        MyCdiContext.checkNotAvailable(cdiContext, beanInstantiator);
        ReceivedEvent.add(ref, new ReceivedEvent(eventType, entity.toString()));
    }
}
