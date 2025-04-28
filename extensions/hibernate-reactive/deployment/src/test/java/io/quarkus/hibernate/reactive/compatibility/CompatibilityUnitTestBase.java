package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.test.vertx.UniAsserter;

public abstract class CompatibilityUnitTestBase {

    public static final String POSTGRES_KIND = "postgresql";
    public static final String USERNAME_PWD = "hibernate_orm_test";
    public static final String SCHEMA_MANAGEMENT_STRATEGY = "drop-and-create";

    public void testReactiveWorks(UniAsserter asserter) {
        Mutiny.SessionFactory mutinySessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();

        asserter.assertThat(() -> mutinySessionFactory.withSession(s -> s.createQuery(
                "from Hero h where h.name = :name", Hero.class)
                .setParameter("name", "Galadriel").getResultList()),
                list -> assertThat(list).hasSize(1));
    }

    public void testBlockingWorks() {
        testBlockingWorks(Optional.empty());
    }

    public void testBlockingWorks(Optional<SessionFactory> injectedSessionFactory) {
        SessionFactory hibernateSessionFactory = injectedSessionFactory
                .orElseGet(() -> Arc.container().instance(SessionFactory.class).get());

        assertThat(hibernateSessionFactory).isNotNull();

        EntityManager entityManager = hibernateSessionFactory.createEntityManager();

        List<Hero> entities = entityManager
                .createQuery("select e from Hero e", Hero.class)
                .getResultList();

        assertThat(entities).isNotEmpty();
        assertThat(entities).hasSize(4);
    }

    public void testReactiveDisabled() {
        Mutiny.SessionFactory mutinySessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();

        assertThat(mutinySessionFactory).isNull();
    }

    public void testBlockingDisabled() {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();

        assertThat(sessionFactory).isNull();
    }
}
