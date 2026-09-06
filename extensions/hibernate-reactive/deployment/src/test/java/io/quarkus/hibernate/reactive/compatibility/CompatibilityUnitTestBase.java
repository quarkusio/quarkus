package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.test.vertx.UniAsserter;

public abstract class CompatibilityUnitTestBase {

    static {
        System.setProperty("user.timezone", "UTC");
    }

    public enum PersistenceMode {
        BLOCKING,
        REACTIVE,
        BOTH
    }

    public void executeCompatibilityTest(PersistenceMode mode, UniAsserter asserter) {
        switch (mode) {
            case BLOCKING -> testBlockingWorks();
            case REACTIVE -> testReactiveWorks(asserter);
            case BOTH -> {
                testBlockingWorks();
                testReactiveWorks(asserter);
            }
        }
    }

    public static final String POSTGRES_KIND = "postgresql";
    public static final String USERNAME_PWD = "hibernate_orm_test";
    public static final String SCHEMA_MANAGEMENT_STRATEGY = "drop-and-create";

    public void testReactiveWorks(UniAsserter asserter) {
        Mutiny.SessionFactory mutinySessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();
        testReactiveWorks(mutinySessionFactory, asserter);
    }

    public void testReactiveWorks(Mutiny.SessionFactory mutinySessionFactory, UniAsserter asserter) {
        asserter.assertThat(() -> mutinySessionFactory.withSession(s -> s.createQuery(
                "from Hero h where h.name = :name", Hero.class)
                .setParameter("name", "Galadriel").getResultList()),
                list -> assertThat(list).hasSize(1));
    }

    public void testBlockingWorks() {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();
        testBlockingWorks(sessionFactory);
    }

    public void testBlockingWorks(SessionFactory hibernateSessionFactory) {
        assertThat(hibernateSessionFactory).isNotNull();

        EntityManager entityManager = hibernateSessionFactory.createEntityManager();

        List<Hero> entities = entityManager
                .createQuery("select e from Hero e", Hero.class)
                .getResultList();

        assertThat(entities).isNotEmpty();
        assertThat(entities).hasSize(4);
    }

    public void testBlockingHeroExists(String heroName) {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();
        assertThat(sessionFactory).isNotNull();

        EntityManager entityManager = sessionFactory.createEntityManager();

        List<Hero> entities = entityManager
                .createQuery("select e from Hero e where e.name = :name", Hero.class)
                .setParameter("name", heroName)
                .getResultList();

        assertThat(entities).extracting("name").containsExactly(heroName);
    }

    public void testReactiveDisabled() {
        Mutiny.SessionFactory mutinySessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();

        assertThat(mutinySessionFactory).isNull();
    }

    public void testReactiveDisabled(String persistenceUnitName) {
        Mutiny.SessionFactory mutinySessionFactory = Arc.container()
                .instance(Mutiny.SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName))
                .get();

        assertThat(mutinySessionFactory).isNull();
    }

    public void testBlockingDisabled() {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();

        assertThat(sessionFactory).isNull();
    }

    public void testBlockingDisabled(String persistenceUnitName) {
        SessionFactory sessionFactory = Arc.container()
                .instance(SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName))
                .get();

        assertThat(sessionFactory).isNull();
    }
}
