package io.quarkus.hibernate.reactive.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.transaction.TransactionalInterceptorRequired;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

public class HibernateReactiveTransactionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Hero.class)
                    .addClasses(TransactionalInterceptorRequired.class)
                    .addAsResource("initialTransactionData.sql", "import.sql"))
            .withConfigurationResource("application-reactive-transaction.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Pool pool;

    /**
     * This test shows how to use hibernate reactive .withTransaction to set transactional boundaries
     * Below there's testReactiveAnnotationTransaction which is the same test but with @Transactional
     *
     * @param asserter
     */
    @Test
    @RunOnVertxContext
    public void testReactiveManualTransaction(UniAsserter asserter) {
        // initialTransactionData.sql
        Long heroId = 60L;

        int originalPoolSize = pool.size();

        // First update, make sure it's committed
        asserter.assertThat(
                () -> sessionFactory.withTransaction(session -> updateHero(session, heroId, "updatedNameCommitted"))
                        // 2nd endpoint call
                        .chain(() -> sessionFactory.withTransaction(session -> session.find(Hero.class, heroId))),
                h -> assertThat(h.name).isEqualTo("updatedNameCommitted"));

        // Second update, make sure there's a rollback
        asserter.assertThat(
                () -> sessionFactory.withTransaction(session -> {
                    return updateHero(session, heroId, "this name won't appear")
                            .onItem().invoke(h -> {
                                throw new RuntimeException("Failing update");
                            });
                }).onFailure().recoverWithNull()
                        .chain(() -> sessionFactory.withTransaction(session -> session.find(Hero.class, heroId))),
                h -> {
                    assertThat(h.name).isEqualTo("updatedNameCommitted");
                });

        // Verify pool size is back to initial (connection returned)
        asserter.execute(() -> {
            int nowSize = pool.size();
            assertThat(originalPoolSize).isEqualTo(nowSize);
        });

    }

    @Inject
    Mutiny.Session session;

    /*
     * This is the same test as #testReactiveManualTransaction but instead of manually calling sessionFactory.withTransaction
     * We use the annotation @Transactional
     */
    @Test
    @RunOnVertxContext
    public void testReactiveAnnotationTransaction(UniAsserter asserter) {
        // initialTransactionData.sql
        Long heroId = 50L;

        int originalPoolSize = pool.size();

        // First update, make sure it's committed
        asserter.assertThat(
                () -> updateWithCommit(heroId, "updatedNameCommitted")
                        .chain(() -> findHero(heroId)),
                h -> {
                    assertThat(h.name).isEqualTo("updatedNameCommitted");
                });

        // Second update, make sure there's a rollback
        asserter.assertThat(
                () -> transactionalUpdateWithRollback(heroId, "this name won't appear")
                        .onFailure().recoverWithNull()
                        .chain(() -> findHero(heroId)),
                h -> {
                    assertThat(h.name).isEqualTo("updatedNameCommitted");
                });

        // Verify pool size is back to initial (connection returned)
        // Actually with quarkus.datasource.reactive.max-size=1 specified in application-reactive-transaction.properties
        // if we don't close the connections the test will hang while acquiring the new connection after the first update
        // Better test it anyway
        asserter.execute(() -> {
            int nowSize = pool.size();
            assertThat(originalPoolSize).isEqualTo(nowSize);
        });
    }


    @Test
    @RunOnVertxContext
    public void testReactiveAnnotationTransactionWithTwoMethods(UniAsserter asserter) {
        // initialTransactionData.sql
        Long heroId = 50L;

        asserter.assertThat(
                () -> updateCallingAnotherTransactionalMethod(heroId, "updatedNameTwiceCommitted")
                        .chain(() -> findHero(heroId)),
                h -> {
                    assertThat(h.name).isEqualTo("updatedNameTwiceCommitted");
                });

    }

    @Transactional
    public Uni<Hero> findHero(Long heroId) {
        return session.find(Hero.class, heroId);
    }

    @Transactional
    public Uni<Hero> updateWithCommit(Long heroId, String newName) {
        return updateHero(session, heroId, newName);
    }

    @Transactional
    public Uni<Hero> updateCallingAnotherTransactionalMethod(Long heroId, String newName) {
        return updateHero(session, heroId, newName + "thisShouldntAppear")
                .chain(h -> updateWithCommit(heroId, newName));
    }

    @Transactional
    public Uni<Hero> transactionalUpdateWithRollback(Long heroId, String newName) {
        return updateHero(session, heroId, newName)
                .onItem().invoke(h -> {
                    throw new RuntimeException("Failing update");
                });
    }

    public Uni<Hero> updateHero(Mutiny.Session session, Long id, String newName) {
        return session.find(Hero.class, id)
                .map(h -> {
                    h.setName(newName);
                    return h;
                });
    }
}
