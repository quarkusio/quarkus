package io.quarkus.hibernate.orm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class JpaListenerOnPrivateMethodOfSingletonCdiBeanTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties"));

    @Inject
    EventStore eventStore;

    @Inject
    EntityManager em;

    @Test
    @Transactional
    public void test() {
        Arc.container().requestContext().activate();
        try {
            SomeEntity entity = new SomeEntity("test");
            em.persist(entity);
            em.flush();
        } finally {
            Arc.container().requestContext().terminate();
        }

        assertThat(eventStore.getEvents()).containsExactly("prePersist", "postPersist");
    }

    @Entity
    @EntityListeners(SomeEntityListener.class)
    public static class SomeEntity {
        private long id;
        private String name;

        public SomeEntity() {
        }

        public SomeEntity(String name) {
            this.name = name;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myEntitySeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "SomeEntity:" + name;
        }
    }

    @Singleton
    public static class SomeEntityListener {

        private final EventStore eventStore;

        public SomeEntityListener(EventStore eventStore) {
            this.eventStore = eventStore;
        }

        @PrePersist
        void prePersist(SomeEntity someEntity) {
            eventStore.addEvent("prePersist");
        }

        @PostPersist
        private void postPersist(SomeEntity someEntity) {
            eventStore.addEvent("postPersist");
        }
    }

    @ApplicationScoped
    public static class EventStore {
        private final List<String> events = new CopyOnWriteArrayList<>();

        public void addEvent(String event) {
            events.add(event);
        }

        public List<String> getEvents() {
            return events;
        }
    }
}
