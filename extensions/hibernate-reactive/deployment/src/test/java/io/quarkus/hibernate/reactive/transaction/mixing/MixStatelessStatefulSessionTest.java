package io.quarkus.hibernate.reactive.transaction.mixing;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.transaction.Hero;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

/**
 * Verifies that Hibernate Reactive allows mixing regular Session and StatelessSession
 * within the same transaction.
 */
public class MixStatelessStatefulSessionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Hero.class))
            .withConfigurationResource("application-reactive-transaction.properties");

    @Inject
    Mutiny.Session session;

    @Inject
    Mutiny.StatelessSession statelessSession;

    @Inject
    Pool pool;

    @Test
    @RunOnVertxContext
    public void testRegularSessionThenStatelessSessionInTransactional(UniAsserter asserter) {
        // Use regular Session, then StatelessSession in same transaction - should work without error
        asserter.execute(() -> assertThat(pool.size()).isEqualTo(1));
        asserter.assertThat(
                () -> transactionalMethodUsingRegularSessionThenStatelessSession(),
                count -> assertThat(count).isNotNull());
        // Verify pool size is still 1 (connection was shared and released)
        asserter.execute(() -> assertThat(pool.size()).isEqualTo(1));
    }

    @Test
    @RunOnVertxContext
    public void testStatelessSessionThenRegularSessionInTransactional(UniAsserter asserter) {
        // Use StatelessSession, then regular Session in same transaction - should work without error
        asserter.execute(() -> assertThat(pool.size()).isEqualTo(1));
        asserter.assertThat(
                () -> transactionalMethodUsingStatelessSessionThenRegularSession(),
                count -> assertThat(count).isNotNull());
        // Verify pool size is still 1 (connection was shared and released)
        asserter.execute(() -> assertThat(pool.size()).isEqualTo(1));
    }

    @Test
    @RunOnVertxContext
    public void testBothSessionsCommitTogether(UniAsserter asserter) {
        // Make changes via both sessions, verify both commit together
        asserter.assertThat(
                () -> transactionalMethodCreatingViaBothSessions("CommitHero1", "CommitHero2"),
                v -> assertThat(v).isNull());

        // Verify both were persisted
        asserter.assertThat(
                () -> transactionalMethodCountingHeroes("CommitHero%"),
                count -> assertThat(count).isEqualTo(2L));
    }

    @Test
    @RunOnVertxContext
    public void testBothSessionsRollbackTogether(UniAsserter asserter) {
        // Make changes via both sessions, then throw exception - verify both rolled back
        asserter.assertFailedWith(
                () -> transactionalMethodCreatingViaBothSessionsThenFailing("RollbackHero1", "RollbackHero2"),
                e -> assertThat(e)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Force rollback"));

        // Verify neither was persisted
        asserter.assertThat(
                () -> transactionalMethodCountingHeroes("RollbackHero%"),
                count -> assertThat(count).isEqualTo(0L));
    }

    @Transactional
    public Uni<Long> transactionalMethodUsingRegularSessionThenStatelessSession() {
        return session.createSelectionQuery("select count(h) from Hero h", Long.class).getSingleResult()
                .chain(count -> statelessSession.createSelectionQuery("select count(h) from Hero h", Long.class)
                        .getSingleResult());
    }

    @Transactional
    public Uni<Long> transactionalMethodUsingStatelessSessionThenRegularSession() {
        return statelessSession.createSelectionQuery("select count(h) from Hero h", Long.class).getSingleResult()
                .chain(count -> session.createSelectionQuery("select count(h) from Hero h", Long.class)
                        .getSingleResult());
    }

    @Transactional
    public Uni<Void> transactionalMethodCreatingViaBothSessions(String name1, String name2) {
        Hero hero1 = new Hero(name1);
        return session.persist(hero1)
                .call(() -> session.flush())
                .chain(() -> {
                    Hero hero2 = new Hero(name2);
                    return statelessSession.insert(hero2);
                });
    }

    @Transactional
    public Uni<Void> transactionalMethodCreatingViaBothSessionsThenFailing(String name1, String name2) {
        return transactionalMethodCreatingViaBothSessions(name1, name2)
                .chain(() -> Uni.createFrom().failure(new RuntimeException("Force rollback")));
    }

    @Transactional
    public Uni<Long> transactionalMethodCountingHeroes(String namePattern) {
        return session.createSelectionQuery("select count(h) from Hero h where h.name like :name", Long.class)
                .setParameter("name", namePattern)
                .getSingleResult();
    }

}
