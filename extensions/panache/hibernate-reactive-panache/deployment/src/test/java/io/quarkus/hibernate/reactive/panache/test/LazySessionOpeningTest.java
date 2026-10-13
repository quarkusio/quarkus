package io.quarkus.hibernate.reactive.panache.test;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

/**
 * Calling a {@code @WithSession} / {@code @WithTransaction} method, or {@code Panache.withSession()} /
 * {@code Panache.withTransaction()}, must only assemble a {@link Uni}: the session is opened when the {@link Uni} is
 * subscribed.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57396">#57396</a>
 */
public class LazySessionOpeningTest {

    @RegisterExtension
    static QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MyEntity.class, MyEntityRepository.class));

    @Inject
    MyEntityRepository repository;

    @Test
    @RunOnVertxContext
    public void withSessionDoesNotOpenSessionBeforeSubscription(UniAsserter asserter) {
        asserter.execute(() -> {
            assertNoSession();
            Uni<Long> uni = repository.countWithSession();
            assertNoSession();
            return uni;
        });
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void withTransactionDoesNotOpenSessionBeforeSubscription(UniAsserter asserter) {
        asserter.execute(() -> {
            assertNoSession();
            Uni<Long> uni = repository.countWithTransaction();
            assertNoSession();
            return uni;
        });
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void withStatelessSessionDoesNotOpenSessionBeforeSubscription(UniAsserter asserter) {
        asserter.execute(() -> {
            assertNoSession();
            Uni<Long> uni = repository.countWithStatelessSession();
            assertNoSession();
            return uni;
        });
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void programmaticWithSessionDoesNotOpenSessionBeforeSubscription(UniAsserter asserter) {
        asserter.execute(() -> {
            assertNoSession();
            Uni<Long> uni = Panache.withSession(repository::count);
            assertNoSession();
            return uni;
        });
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void programmaticWithTransactionDoesNotOpenSessionBeforeSubscription(UniAsserter asserter) {
        asserter.execute(() -> {
            assertNoSession();
            Uni<Long> uni = Panache.withTransaction(repository::count);
            assertNoSession();
            return uni;
        });
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void withSessionUniCanBeSubscribedTwice(UniAsserter asserter) {
        asserter.assertThat(() -> {
            Uni<Long> uni = repository.countWithSession();
            return uni.chain(first -> uni);
        }, count -> assertThat(count).isZero());
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void withTransactionUniCanBeSubscribedTwice(UniAsserter asserter) {
        asserter.assertThat(() -> {
            Uni<Long> uni = repository.countWithTransaction();
            return uni.chain(first -> uni);
        }, count -> assertThat(count).isZero());
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void withSessionUniCanBeRetried(UniAsserter asserter) {
        int[] attempts = { 0 };
        asserter.assertThat(() -> repository.failOnFirstAttempt(attempts).onFailure().retry().atMost(1),
                count -> {
                    assertThat(count).isZero();
                    assertThat(attempts[0]).isEqualTo(2);
                });
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    @Test
    @RunOnVertxContext
    public void discardedUniLeavesNoSessionBehind(UniAsserter asserter) {
        asserter.execute(() -> {
            // built, but never subscribed
            repository.countWithSession();
            repository.countWithTransaction();
            assertNoSession();
        });
        asserter.assertThat(repository::countWithSession, count -> assertThat(count).isZero());
        asserter.execute(LazySessionOpeningTest::assertNoSession);
    }

    private static void assertNoSession() {
        assertThat(SessionOperations.getCurrentSession(DEFAULT_PERSISTENCE_UNIT_NAME)).isNull();
        assertThat(SessionOperations.getCurrentStatelessSession(DEFAULT_PERSISTENCE_UNIT_NAME)).isNull();
    }

    @ApplicationScoped
    public static class MyEntityRepository implements PanacheRepository<MyEntity> {

        @WithSession
        public Uni<Long> countWithSession() {
            return count();
        }

        @WithTransaction
        public Uni<Long> countWithTransaction() {
            return count();
        }

        @WithSession(stateless = true)
        public Uni<Long> countWithStatelessSession() {
            return SessionOperations.getStatelessSession()
                    .chain(s -> s.createSelectionQuery("select count(*) from MyEntity", Long.class).getSingleResult());
        }

        @WithSession
        public Uni<Long> failOnFirstAttempt(int[] attempts) {
            return count().invoke(() -> {
                // Only the first attemp will throw an exception
                if (++attempts[0] == 1) {
                    throw new IllegalStateException("Transient failure");
                }
            });
        }
    }
}
