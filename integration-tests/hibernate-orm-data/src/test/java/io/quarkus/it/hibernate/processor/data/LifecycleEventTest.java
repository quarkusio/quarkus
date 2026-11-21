package io.quarkus.it.hibernate.processor.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.data.event.LifecycleEvent;
import jakarta.data.event.PostDeleteEvent;
import jakarta.data.event.PostInsertEvent;
import jakarta.data.event.PreDeleteEvent;
import jakarta.data.event.PreInsertEvent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import io.quarkus.it.hibernate.processor.data.pudefault.LifecycleEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LifecycleEventTest {

    @Inject
    EntityManager entityManager;

    @Inject
    Observer observer;

    @Test
    @TestTransaction
    public void generatedListenerUsesTypedEventsAndDeliversThem() throws Exception {
        Class<?> metamodel = Class.forName("io.quarkus.it.hibernate.processor.data.pudefault._LifecycleEntity");
        long typedEvents = Arrays.stream(metamodel.getDeclaredFields())
                .filter(field -> field.getType() == Event.class)
                .filter(field -> field.getGenericType() instanceof ParameterizedType parameterized
                        && parameterized.getActualTypeArguments()[0] instanceof ParameterizedType eventType
                        && eventType.getRawType() instanceof Class<?> raw
                        && raw != LifecycleEvent.class
                        && LifecycleEvent.class.isAssignableFrom(raw))
                .count();
        assertThat(typedEvents).isEqualTo(8);
        assertThat(metamodel.getDeclaredField("__quarkusLifecyclePreInsert")).isNotNull();

        observer.events.clear();
        observer.preInsertIds.clear();
        LifecycleEntity entity = new LifecycleEntity();
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.remove(entity);
        entityManager.flush();
        assertThat(observer.preInsertIds).containsExactly(entity.id);
        assertThat(observer.events).containsExactly(PreInsertEvent.class, PostInsertEvent.class,
                PreDeleteEvent.class, PostDeleteEvent.class);
    }

    @Singleton
    public static class Observer {
        final List<Long> preInsertIds = new ArrayList<>();
        final List<Class<?>> events = new ArrayList<>();

        void specific(@Observes PreInsertEvent<LifecycleEntity> event) {
            preInsertIds.add(event.entity().id);
        }

        void lifecycle(@Observes LifecycleEvent<?> event) {
            events.add(event.getClass());
        }
    }
}
