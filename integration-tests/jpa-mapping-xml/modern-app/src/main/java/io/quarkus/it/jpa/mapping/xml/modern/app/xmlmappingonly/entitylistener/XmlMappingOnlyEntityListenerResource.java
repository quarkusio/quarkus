package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.entitylistener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;

import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/xml-mapping-only/entity-listener")
@ApplicationScoped
public class XmlMappingOnlyEntityListenerResource {

    @Inject
    @PersistenceUnit("xml-mapping-only-entity-listener")
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @GET
    @Path("/entity-listeners-annotation")
    @Produces(MediaType.TEXT_PLAIN)
    public String entityListenersAnnotation() throws Exception {
        return doTest(EntityWithListenerThroughEntityListenersAnnotation.class,
                entityId -> ReceivedEvent.objectRef(MyListenerRequiringCdi.class, 0),
                EntityWithListenerThroughEntityListenersAnnotation::new,
                EntityWithListenerThroughEntityListenersAnnotation::setId,
                EntityWithListenerThroughEntityListenersAnnotation::setText);
    }

    @GET
    @Path("/entity-instance-methods")
    @Produces(MediaType.TEXT_PLAIN)
    public String entityInstanceMethods() throws Exception {
        return doTest(EntityWithListenerThroughInstanceMethods.class,
                entityId -> ReceivedEvent.objectRef(EntityWithListenerThroughInstanceMethods.class, entityId),
                EntityWithListenerThroughInstanceMethods::new,
                EntityWithListenerThroughInstanceMethods::setId, EntityWithListenerThroughInstanceMethods::setText);
    }

    private <T> String doTest(Class<T> entityClass, Function<Integer, String> expectedListenerRefFunction,
            Supplier<T> entityConstructor, BiConsumer<T, Integer> entitySetId, BiConsumer<T, String> entitySetText)
            throws Exception {
        ReceivedEvent.clear();

        int entityId = 42;
        String entityRef = ReceivedEvent.objectRef(entityClass, entityId);

        try {
            transaction.begin();
            T entity = entityConstructor.get();
            entitySetId.accept(entity, entityId);
            entitySetText.accept(entity, "initial");
            em.persist(entity);
            transaction.commit();
            String expectedListenerRef = expectedListenerRefFunction.apply(entityId);
            assertThatReceivedEvents(expectedListenerRef)
                    .containsExactly(
                            new ReceivedEvent(PrePersist.class, entityRef),
                            new ReceivedEvent(PostPersist.class, entityRef));

            ReceivedEvent.clear();
            transaction.begin();
            entity = em.find(entityClass, entityId);
            entitySetText.accept(entity, "new");
            transaction.commit();
            expectedListenerRef = expectedListenerRefFunction.apply(entityId);
            assertThatReceivedEvents(expectedListenerRef)
                    .containsExactly(
                            new ReceivedEvent(PostLoad.class, entityRef),
                            new ReceivedEvent(PreUpdate.class, entityRef),
                            new ReceivedEvent(PostUpdate.class, entityRef));

            ReceivedEvent.clear();
            transaction.begin();
            entity = em.find(entityClass, entityId);
            em.remove(entity);
            transaction.commit();
            expectedListenerRef = expectedListenerRefFunction.apply(entityId);
            assertThatReceivedEvents(expectedListenerRef)
                    .containsExactly(
                            new ReceivedEvent(PostLoad.class, entityRef),
                            new ReceivedEvent(PreRemove.class, entityRef),
                            new ReceivedEvent(PostRemove.class, entityRef));
        } catch (Exception | AssertionError e) {
            try {
                transaction.rollback();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }

        return "OK";
    }

    private ListAssert<Object> assertThatReceivedEvents(String listenerRef) {
        return assertThat(ReceivedEvent.get())
                .as("Received events")
                .containsOnlyKeys(listenerRef)
                .extractingByKey(listenerRef, InstanceOfAssertFactories.LIST)
                .as("Received events for " + listenerRef);
    }
}
