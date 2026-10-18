package io.quarkus.hibernate.orm.temporal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TemporalEntityTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TemporalTestEntity.class))
            .withConfigurationResource("application.properties");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void testTemporalEntityCrudAndTimeTravel() throws InterruptedException {
        sessionFactory.inTransaction(session -> {
            session.persist(new TemporalTestEntity(1L, "original"));
        });

        Thread.sleep(100);
        var instant = Instant.now();
        Thread.sleep(100);

        sessionFactory.inTransaction(session -> {
            var entity = session.find(TemporalTestEntity.class, 1L);
            entity.setName("updated");
        });

        sessionFactory.inSession(session -> {
            var entity = session.find(TemporalTestEntity.class, 1L);
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("updated");
        });

        try (var session = sessionFactory.withOptions().asOf(instant).open()) {
            var entity = session.find(TemporalTestEntity.class, 1L);
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("original");
        }
    }

    @Test
    public void testTemporalEntityDeleteAndTimeTravel() throws InterruptedException {
        sessionFactory.inTransaction(session -> {
            session.persist(new TemporalTestEntity(2L, "to-delete"));
        });

        Thread.sleep(100);
        var instant = Instant.now();
        Thread.sleep(100);

        sessionFactory.inTransaction(session -> {
            var entity = session.find(TemporalTestEntity.class, 2L);
            session.remove(entity);
        });

        sessionFactory.inSession(session -> {
            var entity = session.find(TemporalTestEntity.class, 2L);
            assertThat(entity).isNull();
        });

        try (var session = sessionFactory.withOptions().asOf(instant).open()) {
            var entity = session.find(TemporalTestEntity.class, 2L);
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("to-delete");
        }
    }
}
