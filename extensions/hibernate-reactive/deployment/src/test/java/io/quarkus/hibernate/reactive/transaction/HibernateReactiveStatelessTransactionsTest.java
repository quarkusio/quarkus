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
import io.quarkus.transaction.annotations.Rollback;
import io.smallrye.mutiny.Uni;

public class HibernateReactiveStatelessTransactionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Hero.class)
                    .addClasses(TransactionalInterceptorRequired.class)
                    .addAsResource("initialTransactionData.sql", "import.sql"))
            .withConfigurationResource("application-reactive-transaction.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * This test shows how to use hibernate reactive .withStatelessTransaction to set transactional boundaries
     * Below there's testReactiveAnnotationTransaction which is the same test but with @Transactional
     *
     * @param asserter
     */
    @Test
    @RunOnVertxContext
    public void testReactiveManualTransaction(UniAsserter asserter) {
        // initialTransactionData.sql
        Long heroId = 50L;

        // First update, make sure it's committed
        asserter.assertThat(
                () -> sessionFactory.withStatelessTransaction(session -> updateHero(session, heroId, "updatedNameCommitted"))
                        // 2nd endpoint call
                        .chain(() -> sessionFactory.withStatelessTransaction(session -> session.get(Hero.class, heroId))),
                h -> assertThat(h.name).isEqualTo("updatedNameCommitted"));

        // Second update, make sure there's a rollback
        asserter.assertThat(
                () -> sessionFactory.withStatelessTransaction(session -> {
                    return updateHero(session, heroId, "this name won't appear")
                            .onItem().invoke(h -> {
                                throw new RuntimeException("Failing update");
                            });
                }).onFailure().recoverWithNull()
                        .chain(() -> sessionFactory.withStatelessTransaction(session -> session.get(Hero.class, heroId))),
                h -> {
                    assertThat(h.name).isEqualTo("updatedNameCommitted");
                });
    }

    @Inject
    Mutiny.StatelessSession session;

    /*
     * This is the same test as #testReactiveManualTransaction but instead of manually calling
     * sessionFactory.withStatelessTransaction
     * We use the annotation @Transactional
     */
    @Test
    @RunOnVertxContext
    public void testReactiveAnnotationTransaction(UniAsserter asserter) {
        // initialTransactionData.sql
        Long heroId = 50L;

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
    }

    @Transactional
    public Uni<Hero> findHero(Long previousHeroId) {
        return session.get(Hero.class, previousHeroId);
    }

    @Transactional
    public Uni<Hero> updateWithCommit(Long previousHeroId, String newName) {
        return updateHero(session, previousHeroId, newName);
    }

    @Transactional
    public Uni<Hero> transactionalUpdateWithRollback(Long previousHeroId, String newName) {
        return updateHero(session, previousHeroId, newName)
                .onItem().invoke(h -> {
                    throw new RuntimeException("Failing update");
                });
    }

    public Uni<Hero> updateHero(Mutiny.StatelessSession session, Long id, String newName) {
        return session.get(Hero.class, id)
                .map(h -> {
                    h.setName(newName);
                    return h;
                }).onItem().call(h -> session.update(h));
    }

    @Test
    @RunOnVertxContext
    public void testDontRollbackOnException(UniAsserter asserter) {
        Long heroId = 50L;

        // First update, make sure it's committed
        asserter.assertThat(
                () -> updateWithCommit(heroId, "updatedNameCommitted")
                        .chain(() -> findHero(heroId)),
                h -> assertThat(h.name).isEqualTo("updatedNameCommitted"));

        // Update with dontRollbackOn exception - should commit despite exception
        asserter.assertThat(
                () -> updateWithDontRollbackException(heroId, "committedDespiteException")
                        .onFailure().recoverWithNull()
                        .chain(() -> findHero(heroId)),
                h -> assertThat(h.name).isEqualTo("committedDespiteException"));
    }

    @Test
    @RunOnVertxContext
    public void testRollbackFalseAnnotation(UniAsserter asserter) {
        Long heroId = 50L;

        // First update to set baseline
        asserter.assertThat(
                () -> updateWithCommit(heroId, "baselineName")
                        .chain(() -> findHero(heroId)),
                h -> assertThat(h.name).isEqualTo("baselineName"));

        // Update with @Rollback(false) exception - should commit despite being RuntimeException
        asserter.assertThat(
                () -> updateWithRollbackFalseException(heroId, "committedWithRollbackFalse")
                        .onFailure().recoverWithNull()
                        .chain(() -> findHero(heroId)),
                h -> assertThat(h.name).isEqualTo("committedWithRollbackFalse"));
    }

    @Transactional(dontRollbackOn = DontRollbackException.class)
    public Uni<Hero> updateWithDontRollbackException(Long heroId, String newName) {
        return updateHero(session, heroId, newName)
                .onItem().invoke(h -> {
                    throw new DontRollbackException("This should not trigger rollback");
                });
    }

    @Transactional
    public Uni<Hero> updateWithRollbackFalseException(Long heroId, String newName) {
        return updateHero(session, heroId, newName)
                .onItem().invoke(h -> {
                    throw new NoRollbackException("Exception with @Rollback(false)");
                });
    }

    static class DontRollbackException extends RuntimeException {
        public DontRollbackException(String message) {
            super(message);
        }
    }

    static class CheckedExceptionWrapper extends Exception {

    }

    @Rollback(false)
    static class NoRollbackException extends RuntimeException {
        public NoRollbackException(String message) {
            super(message);
        }
    }
}
